package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.AnnouncementRepository;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.OfferingAccess;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Announcements of an offering.
 * Who can read: the professor (and admins), the TAs, and the students who are enrolled.
 * Who can post: the professor (and admins), and TAs with "Post announcements".
 * Who can delete or edit: the professor (and admins) and TAs with "Manage all announcements" any announcement,
 * TAs with "Post announcements" only their own.
 * An edited announcement shows an "edited" time. Editing tells the students again (one notification per announcement).
 * A deleted announcement is only hidden (the history stays in the database) and cannot be brought back.
 */
@Controller
public class AnnouncementController {

    private static final int TITLE_MAX = 100;
    private static final int BODY_MAX = 2000;

    // Announcements newer than this many days get a "New" tag.
    private static final long NEW_FOR_DAYS = 3;

    // Users are in Iran, so times are shown in Tehran time.
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Tehran"));

    /** One announcement as the page shows it. */
    public record AnnouncementView(Announcement announcement, String when, String editedWhen,
                                   boolean recent, boolean canDelete, boolean canEdit) {
    }

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final AnnouncementRepository announcementRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaAssignmentRepository taRepository;
    private final NotificationService notificationService;
    private final OfferingAccess access;

    public AnnouncementController(UserRepository userRepository,
                                  CourseOfferingRepository offeringRepository,
                                  AnnouncementRepository announcementRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  TaAssignmentRepository taRepository,
                                  NotificationService notificationService,
                                  OfferingAccess access) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.announcementRepository = announcementRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taRepository = taRepository;
        this.notificationService = notificationService;
        this.access = access;
    }

    @GetMapping("/courses/{id}/announcements")
    public String list(@PathVariable Long id, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);

        boolean manager = offering.isManagedBy(current);
        boolean ta = isTa(current, offering);
        boolean student = isActiveStudent(current, offering);

        // Only people who belong to this offering can read its announcements.
        if (!(manager || ta || student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // A cancelled offering exists only for the people who manage it.
        if (offering.isCancelled() && !manager) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        boolean open = isOpen(offering);

        List<AnnouncementView> announcements = announcementRepository
                .findByOfferingAndDeletedFalseOrderByCreatedAtDescIdDesc(offering).stream()
                .map(a -> view(a, current, open))
                .toList();

        model.addAttribute("offering", offering);
        model.addAttribute("announcements", announcements);
        model.addAttribute("isCurrentSemester", offering.getSemester().isActive());
        model.addAttribute("isCancelled", offering.isCancelled());
        model.addAttribute("canPost", open && access.can(current, offering, TaPermission.POST_ANNOUNCEMENTS));
        model.addAttribute("canManage", manager);

        return "announcements";
    }

    @PostMapping("/courses/{id}/announcements")
    public String post(@PathVariable Long id,
                       @RequestParam(defaultValue = "") String title,
                       @RequestParam(defaultValue = "") String body,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);

        // Only the professor and the TAs of an open offering can post.
        if (!isOpen(offering) || !access.can(current, offering, TaPermission.POST_ANNOUNCEMENTS)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String cleanTitle = title.trim();
        String cleanBody = body.trim();
        String errorKey = findFirstError(cleanTitle, cleanBody);
        if (errorKey != null) {
            redirect.addFlashAttribute("announcementErrorKey", errorKey);
            return "redirect:/courses/" + id + "/announcements";
        }

        Announcement announcement = new Announcement();
        announcement.setOffering(offering);
        announcement.setAuthor(current);
        announcement.setTitle(cleanTitle);
        announcement.setBody(cleanBody);
        announcement.setCreatedAt(Instant.now());
        announcementRepository.save(announcement);

        // Every enrolled student gets a notification about it.
        notificationService.notifyAnnouncement(announcement);
        return "redirect:/courses/" + id + "/announcements";
    }

    @PostMapping("/announcements/{announcementId}/delete")
    public String delete(@PathVariable Long announcementId, Authentication authentication) {
        User current = currentUser(authentication);
        Announcement announcement = findAnnouncement(announcementId);
        CourseOffering offering = announcement.getOffering();

        if (!isOpen(offering) || !mayChange(announcement, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        announcement.setDeleted(true);
        announcementRepository.save(announcement);

        // Students who have not read it yet are not told about an announcement that no longer exists.
        notificationService.onAnnouncementDeleted(announcement);
        return "redirect:/courses/" + offering.getId() + "/announcements";
    }

    @GetMapping("/announcements/{announcementId}/edit")
    public String editForm(@PathVariable Long announcementId, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        Announcement announcement = findEditable(announcementId, current);

        // After a validation error the page shows what the user typed, not the saved text.
        model.addAttribute("title", model.containsAttribute("draftTitle")
                ? model.getAttribute("draftTitle") : announcement.getTitle());
        model.addAttribute("body", model.containsAttribute("draftBody")
                ? model.getAttribute("draftBody") : announcement.getBody());
        model.addAttribute("announcement", announcement);
        model.addAttribute("offering", announcement.getOffering());
        return "announcement-edit";
    }

    @PostMapping("/announcements/{announcementId}/edit")
    public String edit(@PathVariable Long announcementId,
                       @RequestParam(defaultValue = "") String title,
                       @RequestParam(defaultValue = "") String body,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        User current = currentUser(authentication);
        Announcement announcement = findEditable(announcementId, current);
        Long offeringId = announcement.getOffering().getId();

        String cleanTitle = title.trim();
        String cleanBody = body.trim();
        String errorKey = findFirstError(cleanTitle, cleanBody);
        if (errorKey != null) {
            redirect.addFlashAttribute("announcementErrorKey", errorKey);
            redirect.addFlashAttribute("draftTitle", title);
            redirect.addFlashAttribute("draftBody", body);
            return "redirect:/announcements/" + announcementId + "/edit";
        }

        // Nothing changed: do not mark it as edited.
        if (!cleanTitle.equals(announcement.getTitle()) || !cleanBody.equals(announcement.getBody())) {
            announcement.setTitle(cleanTitle);
            announcement.setBody(cleanBody);
            announcement.setEditedAt(Instant.now());
            announcementRepository.save(announcement);
            // Students are told that the announcement changed.
            notificationService.notifyAnnouncementEdited(announcement);
        }
        return "redirect:/courses/" + offeringId + "/announcements";
    }

    // ---------- helpers ----------

    /**
     * Edit and delete have the same rule: the professor and TAs with "Manage all announcements" any,
     * TAs with "Post announcements" only their own.
     */
    private boolean mayChange(Announcement announcement, User current) {
        CourseOffering offering = announcement.getOffering();
        if (access.can(current, offering, TaPermission.MANAGE_ANNOUNCEMENTS)) {
            return true;
        }
        return announcement.getAuthor().getId().equals(current.getId())
                && access.can(current, offering, TaPermission.POST_ANNOUNCEMENTS);
    }

    private AnnouncementView view(Announcement a, User current, boolean open) {
        boolean canChange = open && mayChange(a, current);
        boolean recent = Duration.between(a.getCreatedAt(), Instant.now()).toDays() < NEW_FOR_DAYS;
        String editedWhen = a.getEditedAt() == null ? null : WHEN.format(a.getEditedAt());
        return new AnnouncementView(a, WHEN.format(a.getCreatedAt()), editedWhen, recent, canChange, canChange);
    }

    /** Returns the message key of the first problem, in the same order as the form fields. */
    private String findFirstError(String title, String body) {
        if (title.isEmpty()) {
            return "announcements.error.titleRequired";
        }
        if (title.length() > TITLE_MAX) {
            return "announcements.error.titleTooLong";
        }
        if (body.isEmpty()) {
            return "announcements.error.bodyRequired";
        }
        if (body.length() > BODY_MAX) {
            return "announcements.error.bodyTooLong";
        }
        return null;
    }

    /** Changes are only allowed in an uncancelled offering of the active semester. */
    private boolean isOpen(CourseOffering offering) {
        return offering.getSemester().isActive() && !offering.isCancelled();
    }

    private boolean isTa(User user, CourseOffering offering) {
        return taRepository.existsByUserAndOfferingAndActiveTrue(user, offering);
    }

    private boolean isActiveStudent(User user, CourseOffering offering) {
        return enrollmentRepository.findByStudentAndOffering(user, offering)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .isPresent();
    }

    /** An announcement the current user may edit, or 404/403. Deleted announcements cannot be edited. */
    private Announcement findEditable(Long announcementId, User current) {
        Announcement announcement = findAnnouncement(announcementId);
        CourseOffering offering = announcement.getOffering();
        if (announcement.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!isOpen(offering) || !mayChange(announcement, current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return announcement;
    }

    private CourseOffering findOffering(Long id) {
        return offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
