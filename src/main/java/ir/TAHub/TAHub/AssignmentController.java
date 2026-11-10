package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Assignment;
import ir.TAHub.TAHub.model.AssignmentExtension;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.StoredFile;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.AssignmentExtensionRepository;
import ir.TAHub.TAHub.repository.AssignmentRepository;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.FileRejectedException;
import ir.TAHub.TAHub.service.FileStorageService;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.OfferingAccess;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Assignments of an offering (phase 4).
 * Who sees them: the people of the offering only - its professor (and admins), its TAs, and its enrolled
 * students. Students see only published ones; drafts are for the professor and TAs.
 * Who creates, edits, publishes, extends and hides: the professor and TAs with "Manage assignments",
 * only in an uncancelled offering of the ACTIVE semester (so a TA of an old semester cannot change it).
 * The questions are one PDF. An assignment cannot be published without it, or with a deadline in the past.
 * An extension is for everyone and has a grade factor (1-100%); it can be done many times.
 */
@Controller
public class AssignmentController {

    private static final int TITLE_MAX = 150;
    private static final int CHAPTER_MAX = 30;
    private static final int DESCRIPTION_MAX = 2000;

    static final ZoneId ZONE = ZoneId.of("Asia/Tehran");
    private static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZONE);
    /** The value format of <input type="datetime-local">. */
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZONE);

    /** One assignment in the list. extendFactor is the factor of the last extension (null if never extended). */
    public record AssignmentRow(Assignment assignment, String deadline, boolean passed, Integer extendFactor) {
    }

    /** One extension in the history on the assignment page. */
    public record ExtensionRow(String from, String to, int factorPercent, String byName, String when) {
    }

    private final CourseOfferingRepository offeringRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentExtensionRepository extensionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final OfferingAccess access;
    private final FileStorageService fileStorage;
    private final NotificationService notificationService;

    public AssignmentController(CourseOfferingRepository offeringRepository,
                                AssignmentRepository assignmentRepository,
                                AssignmentExtensionRepository extensionRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                OfferingAccess access,
                                FileStorageService fileStorage,
                                NotificationService notificationService) {
        this.offeringRepository = offeringRepository;
        this.assignmentRepository = assignmentRepository;
        this.extensionRepository = extensionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.fileStorage = fileStorage;
        this.notificationService = notificationService;
    }

    // ---------- the list ----------

    @GetMapping("/courses/{id}/assignments")
    public String list(@PathVariable Long id, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        requireMember(current, offering);
        boolean staff = isStaff(current, offering);

        List<Assignment> assignments = staff
                ? assignmentRepository.findByOfferingAndDeletedFalseOrderByDeadlineDescIdDesc(offering)
                : assignmentRepository.findByOfferingAndDeletedFalseAndPublishedTrueOrderByDeadlineDescIdDesc(offering);
        Instant now = Instant.now();

        model.addAttribute("offering", offering);
        model.addAttribute("rows", assignments.stream().map(a -> row(a, now)).toList());
        model.addAttribute("isStaff", staff);
        model.addAttribute("canManage", canManage(current, offering));
        model.addAttribute("isCurrentSemester", offering.getSemester().isActive());
        model.addAttribute("isCancelled", offering.isCancelled());
        return "assignments";
    }

    // ---------- create ----------

    @GetMapping("/courses/{id}/assignments/new")
    public String newForm(@PathVariable Long id, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        requireManage(current, offering);
        fillForm(model, offering, null);
        return "assignment-form";
    }

    @PostMapping("/courses/{id}/assignments")
    public String create(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String title,
                         @RequestParam(defaultValue = "") String chapter,
                         @RequestParam(defaultValue = "") String description,
                         @RequestParam(defaultValue = "") String deadline,
                         @RequestParam(required = false) MultipartFile questionFile,
                         @RequestParam(defaultValue = "draft") String action,
                         Authentication authentication,
                         RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        requireManage(current, offering);
        boolean publish = "publish".equals(action);
        String back = "redirect:/courses/" + id + "/assignments/new";

        Instant deadlineAt = parseDeadline(deadline);
        String errorKey = findFirstError(title.trim(), chapter.trim(), description.trim(), deadlineAt);
        if (errorKey == null && publish) {
            errorKey = publishError(FileStorageService.isChosen(questionFile), deadlineAt);
        }
        if (errorKey != null) {
            return backWithError(redirect, back, errorKey, title, chapter, description, deadline);
        }

        StoredFile file = null;
        if (FileStorageService.isChosen(questionFile)) {
            try {
                file = fileStorage.savePdf(questionFile, current);
            } catch (FileRejectedException e) {
                return backWithError(redirect, back, e.getMessageKey(), title, chapter, description, deadline);
            }
        }

        Assignment assignment = new Assignment();
        assignment.setOffering(offering);
        fillText(assignment, title, chapter, description);
        assignment.setDeadline(deadlineAt);
        assignment.setFirstDeadline(deadlineAt);
        assignment.setQuestionFile(file);
        assignment.setCreatedBy(current);
        assignment.setCreatedAt(Instant.now());
        assignmentRepository.save(assignment);

        if (publish) {
            publishNow(assignment);
            redirect.addFlashAttribute("assignmentDoneKey", "assignments.done.published");
        } else {
            redirect.addFlashAttribute("assignmentDoneKey", "assignments.done.draftSaved");
        }
        return "redirect:/assignments/" + assignment.getId();
    }

    // ---------- one assignment ----------

    @GetMapping("/assignments/{assignmentId}")
    public String detail(@PathVariable Long assignmentId, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        Assignment assignment = findVisible(assignmentId, current);
        CourseOffering offering = assignment.getOffering();
        Instant now = Instant.now();

        model.addAttribute("assignment", assignment);
        model.addAttribute("offering", offering);
        model.addAttribute("row", row(assignment, now));
        model.addAttribute("firstDeadline", SHOW.format(assignment.getFirstDeadline()));
        model.addAttribute("isStaff", isStaff(current, offering));
        model.addAttribute("canManage", canManage(current, offering));
        model.addAttribute("hasQuestions", assignment.getQuestionFile() != null);
        model.addAttribute("extensions", extensionRepository.findByAssignmentOrderByCreatedAtAscIdAsc(assignment).stream()
                .map(e -> new ExtensionRow(SHOW.format(e.getPreviousDeadline()), SHOW.format(e.getNewDeadline()),
                        e.getFactorPercent(), e.getCreatedBy().getFullName(), SHOW.format(e.getCreatedAt())))
                .toList());
        // The extend form starts one day after the current deadline (or now, if that is later).
        Instant suggested = (assignment.getDeadline().isAfter(now) ? assignment.getDeadline() : now).plusSeconds(24 * 3600);
        model.addAttribute("suggestedDeadline", INPUT.format(suggested));
        return "assignment-detail";
    }

    @GetMapping("/assignments/{assignmentId}/edit")
    public String editForm(@PathVariable Long assignmentId, Model model, Authentication authentication) {
        User current = currentUser(authentication);
        Assignment assignment = findManageable(assignmentId, current);
        fillForm(model, assignment.getOffering(), assignment);
        return "assignment-form";
    }

    /**
     * Saves changes. The deadline can only be changed while it is a draft; after publishing, use "extend".
     * A newly chosen PDF replaces the old one. action "publish" (drafts only) also publishes it.
     */
    @PostMapping("/assignments/{assignmentId}/edit")
    public String edit(@PathVariable Long assignmentId,
                       @RequestParam(defaultValue = "") String title,
                       @RequestParam(defaultValue = "") String chapter,
                       @RequestParam(defaultValue = "") String description,
                       @RequestParam(defaultValue = "") String deadline,
                       @RequestParam(required = false) MultipartFile questionFile,
                       @RequestParam(defaultValue = "save") String action,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        User current = currentUser(authentication);
        Assignment assignment = findManageable(assignmentId, current);
        boolean draft = !assignment.isPublished();
        boolean publish = draft && "publish".equals(action);
        String back = "redirect:/assignments/" + assignmentId + "/edit";

        Instant deadlineAt = draft ? parseDeadline(deadline) : assignment.getDeadline();
        String errorKey = findFirstError(title.trim(), chapter.trim(), description.trim(), deadlineAt);
        boolean newFile = FileStorageService.isChosen(questionFile);
        if (errorKey == null && publish) {
            errorKey = publishError(newFile || assignment.getQuestionFile() != null, deadlineAt);
        }
        if (errorKey != null) {
            return backWithError(redirect, back, errorKey, title, chapter, description, deadline);
        }

        if (newFile) {
            StoredFile old = assignment.getQuestionFile();
            try {
                assignment.setQuestionFile(fileStorage.savePdf(questionFile, current));
            } catch (FileRejectedException e) {
                return backWithError(redirect, back, e.getMessageKey(), title, chapter, description, deadline);
            }
            if (old != null) {
                assignmentRepository.save(assignment); // nothing points to the old file any more
                fileStorage.delete(old);
            }
        }

        fillText(assignment, title, chapter, description);
        if (draft) {
            assignment.setDeadline(deadlineAt);
            assignment.setFirstDeadline(deadlineAt);
        }
        assignment.setUpdatedAt(Instant.now());
        assignmentRepository.save(assignment);

        if (publish) {
            publishNow(assignment);
            redirect.addFlashAttribute("assignmentDoneKey", "assignments.done.published");
        } else {
            redirect.addFlashAttribute("assignmentDoneKey", "assignments.done.saved");
        }
        return "redirect:/assignments/" + assignmentId;
    }

    @PostMapping("/assignments/{assignmentId}/publish")
    public String publish(@PathVariable Long assignmentId, Authentication authentication, RedirectAttributes redirect) {
        Assignment assignment = findManageable(assignmentId, currentUser(authentication));
        if (assignment.isPublished()) {
            return "redirect:/assignments/" + assignmentId;
        }
        String errorKey = publishError(assignment.getQuestionFile() != null, assignment.getDeadline());
        if (errorKey != null) {
            redirect.addFlashAttribute("assignmentErrorKey", errorKey);
            return "redirect:/assignments/" + assignmentId;
        }
        publishNow(assignment);
        redirect.addFlashAttribute("assignmentDoneKey", "assignments.done.published");
        return "redirect:/assignments/" + assignmentId;
    }

    /** A new deadline for everyone. Answers sent after the first deadline get this extension's factor (phase 5). */
    @PostMapping("/assignments/{assignmentId}/extend")
    public String extend(@PathVariable Long assignmentId,
                         @RequestParam(defaultValue = "") String newDeadline,
                         @RequestParam(defaultValue = "") String factor,
                         Authentication authentication,
                         RedirectAttributes redirect) {
        User current = currentUser(authentication);
        Assignment assignment = findManageable(assignmentId, current);
        if (!assignment.isPublished()) {
            redirect.addFlashAttribute("assignmentErrorKey", "assignments.error.extendDraft");
            return "redirect:/assignments/" + assignmentId;
        }

        Instant newDeadlineAt = parseDeadline(newDeadline);
        Integer factorPercent = parseFactor(factor);
        String errorKey = null;
        if (newDeadlineAt == null) {
            errorKey = "assignments.error.deadlineRequired";
        } else if (!newDeadlineAt.isAfter(assignment.getDeadline())) {
            errorKey = "assignments.error.extendNotLater";
        } else if (!newDeadlineAt.isAfter(Instant.now())) {
            errorKey = "assignments.error.deadlinePast";
        } else if (factorPercent == null || factorPercent < 1 || factorPercent > 100) {
            errorKey = "assignments.error.factor";
        }
        if (errorKey != null) {
            redirect.addFlashAttribute("assignmentErrorKey", errorKey);
            redirect.addFlashAttribute("draftNewDeadline", newDeadline);
            redirect.addFlashAttribute("draftFactor", factor);
            return "redirect:/assignments/" + assignmentId;
        }

        AssignmentExtension extension = new AssignmentExtension();
        extension.setAssignment(assignment);
        extension.setPreviousDeadline(assignment.getDeadline());
        extension.setNewDeadline(newDeadlineAt);
        extension.setFactorPercent(factorPercent);
        extension.setCreatedBy(current);
        extension.setCreatedAt(Instant.now());
        extensionRepository.save(extension);

        assignment.setDeadline(newDeadlineAt);
        assignment.setUpdatedAt(Instant.now());
        assignmentRepository.save(assignment);

        notificationService.notifyAssignmentExtended(assignment);
        redirect.addFlashAttribute("assignmentDoneKey", "assignments.done.extended");
        return "redirect:/assignments/" + assignmentId;
    }

    /** Deleting only hides it (answers and grades will hang on it later). Its notifications are removed. */
    @PostMapping("/assignments/{assignmentId}/hide")
    public String hide(@PathVariable Long assignmentId, Authentication authentication) {
        Assignment assignment = findManageable(assignmentId, currentUser(authentication));
        assignment.setDeleted(true);
        assignment.setUpdatedAt(Instant.now());
        assignmentRepository.save(assignment);
        notificationService.onAssignmentHidden(assignment);
        return "redirect:/courses/" + assignment.getOffering().getId() + "/assignments";
    }

    /**
     * The question PDF, opened in the browser. Only for the people of the offering (and a draft's PDF only
     * for the professor and TAs). The file name is made from the course code, the title and the chapter.
     */
    @GetMapping("/assignments/{assignmentId}/question.pdf")
    public ResponseEntity<Resource> questionPdf(@PathVariable Long assignmentId, Authentication authentication) {
        User current = currentUser(authentication);
        Assignment assignment = findVisible(assignmentId, current);
        StoredFile file = assignment.getQuestionFile();
        if (file == null || !fileStorage.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String name = FileStorageService.downloadName(assignment.getOffering().getCourse().getCode()
                + " - " + assignment.getTitle() + " - " + assignment.getChapter());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new FileSystemResource(fileStorage.pathOf(file)));
    }

    // ---------- helpers ----------

    private void publishNow(Assignment assignment) {
        Instant now = Instant.now();
        assignment.setPublished(true);
        assignment.setPublishedAt(now);
        assignment.setFirstDeadline(assignment.getDeadline());
        assignment.setUpdatedAt(now);
        assignmentRepository.save(assignment);
        notificationService.notifyAssignmentPublished(assignment);
    }

    private AssignmentRow row(Assignment assignment, Instant now) {
        Integer factor = assignment.isExtended()
                ? extensionRepository.findFirstByAssignmentOrderByCreatedAtDescIdDesc(assignment)
                        .map(AssignmentExtension::getFactorPercent).orElse(null)
                : null;
        return new AssignmentRow(assignment, SHOW.format(assignment.getDeadline()),
                !now.isBefore(assignment.getDeadline()), factor);
    }

    /** The form's values: what was typed (after an error), else the saved assignment, else empty. */
    private void fillForm(Model model, CourseOffering offering, Assignment assignment) {
        model.addAttribute("offering", offering);
        model.addAttribute("assignment", assignment);
        model.addAttribute("isNew", assignment == null);
        model.addAttribute("isDraft", assignment == null || !assignment.isPublished());
        model.addAttribute("formTitle", draftOr(model, "draftTitle", assignment == null ? "" : assignment.getTitle()));
        model.addAttribute("formChapter", draftOr(model, "draftChapter", assignment == null ? "" : assignment.getChapter()));
        model.addAttribute("formDescription", draftOr(model, "draftDescription",
                assignment == null || assignment.getDescription() == null ? "" : assignment.getDescription()));
        model.addAttribute("formDeadline", draftOr(model, "draftDeadline",
                assignment == null ? "" : INPUT.format(assignment.getDeadline())));
        model.addAttribute("currentDeadline", assignment == null ? null : SHOW.format(assignment.getDeadline()));
        model.addAttribute("maxMb", FileStorageService.MAX_PDF_BYTES / (1024 * 1024));
    }

    private String backWithError(RedirectAttributes redirect, String back, String errorKey,
                                 String title, String chapter, String description, String deadline) {
        redirect.addFlashAttribute("assignmentErrorKey", errorKey);
        redirect.addFlashAttribute("draftTitle", title);
        redirect.addFlashAttribute("draftChapter", chapter);
        redirect.addFlashAttribute("draftDescription", description);
        redirect.addFlashAttribute("draftDeadline", deadline);
        return back;
    }

    private void fillText(Assignment assignment, String title, String chapter, String description) {
        assignment.setTitle(title.trim());
        assignment.setChapter(chapter.trim().replaceAll("\\s+", " "));
        assignment.setDescription(description.trim().isEmpty() ? null : description.trim());
    }

    /** The message key of the first problem, in the order of the form fields (null = no problem). */
    private String findFirstError(String title, String chapter, String description, Instant deadline) {
        if (title.isEmpty()) {
            return "assignments.error.titleRequired";
        }
        if (title.length() > TITLE_MAX) {
            return "assignments.error.titleTooLong";
        }
        if (chapter.isEmpty()) {
            return "assignments.error.chapterRequired";
        }
        if (chapter.length() > CHAPTER_MAX) {
            return "assignments.error.chapterTooLong";
        }
        if (description.length() > DESCRIPTION_MAX) {
            return "assignments.error.descriptionTooLong";
        }
        if (deadline == null) {
            return "assignments.error.deadlineRequired";
        }
        return null;
    }

    /** What is still missing before it can be published (null = it can be published). */
    private String publishError(boolean hasPdf, Instant deadline) {
        if (!hasPdf) {
            return "assignments.error.pdfRequired";
        }
        if (!deadline.isAfter(Instant.now())) {
            return "assignments.error.deadlinePast";
        }
        return null;
    }

    /** "2026-11-20T23:59" (Tehran time, from the browser) -> Instant. null if empty or not valid. */
    private Instant parseDeadline(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim()).atZone(ZONE).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Integer parseFactor(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Object draftOr(Model model, String draftName, String saved) {
        return model.containsAttribute(draftName) ? model.getAttribute(draftName) : saved;
    }

    /** The professor (and admins) or an active TA of the offering. */
    private boolean isStaff(User user, CourseOffering offering) {
        return access.isManager(user, offering) || access.activeTa(user, offering).isPresent();
    }

    private boolean isEnrolled(User user, CourseOffering offering) {
        return enrollmentRepository.findByStudentAndOffering(user, offering)
                .map(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .orElse(false);
    }

    /** Only the people of the offering. A cancelled offering exists only for its managers. */
    private void requireMember(User user, CourseOffering offering) {
        if (offering.isCancelled() && !access.isManager(user, offering)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!isStaff(user, offering) && !isEnrolled(user, offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /** The professor and TAs with "Manage assignments", in an uncancelled offering of the active semester. */
    private boolean canManage(User user, CourseOffering offering) {
        return access.isOpen(offering) && access.can(user, offering, TaPermission.MANAGE_ASSIGNMENTS);
    }

    private void requireManage(User user, CourseOffering offering) {
        if (!canManage(user, offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /** An assignment this user may see: a member of its offering; drafts only for the professor and TAs. */
    private Assignment findVisible(Long assignmentId, User user) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireMember(user, assignment.getOffering());
        if (!assignment.isPublished() && !isStaff(user, assignment.getOffering())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return assignment;
    }

    private Assignment findManageable(Long assignmentId, User user) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireManage(user, assignment.getOffering());
        return assignment;
    }

    private CourseOffering findOffering(Long id) {
        return offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
