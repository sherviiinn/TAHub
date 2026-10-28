package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.SiteNotice;
import ir.TAHub.TAHub.model.SiteNoticeDismissal;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.SiteNoticeDismissalRepository;
import ir.TAHub.TAHub.repository.SiteNoticeRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Notices from the admins to everyone.
 * Everyone who is logged in reads them at /notices and can close the top bar for themselves.
 * Only admins write, edit, notify everyone again, take off the bar and delete (all under /admin/**, which SecurityConfig
 * already allows for admins only). Admins see the form and the buttons on the same /notices page.
 */
@Controller
public class SiteNoticeController {

    private static final int TITLE_MAX = 100;
    private static final int BODY_MAX = 2000;

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Tehran"));

    /** One notice as the page shows it. */
    public record NoticeView(SiteNotice notice, String when, String editedWhen) {
    }

    private final SiteNoticeRepository noticeRepository;
    private final SiteNoticeDismissalRepository dismissalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SiteNoticeController(SiteNoticeRepository noticeRepository,
                                SiteNoticeDismissalRepository dismissalRepository,
                                UserRepository userRepository,
                                NotificationService notificationService) {
        this.noticeRepository = noticeRepository;
        this.dismissalRepository = dismissalRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/notices")
    public String list(Model model) {
        List<NoticeView> notices = noticeRepository.findByDeletedFalseOrderByCreatedAtDescIdDesc().stream()
                .map(n -> new NoticeView(n, WHEN.format(n.getCreatedAt()),
                        n.getEditedAt() == null ? null : WHEN.format(n.getEditedAt())))
                .toList();
        model.addAttribute("notices", notices);
        return "notices";
    }

    /** Closes the top bar of one notice for the current user, then goes back to the page they were on. */
    @PostMapping("/notices/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "/") String back,
                          Authentication authentication) {
        User current = currentUser(authentication);
        SiteNotice notice = findNotice(id);
        if (!dismissalRepository.existsByUserAndNotice(current, notice)) {
            dismissalRepository.save(new SiteNoticeDismissal(current, notice));
        }
        return "redirect:" + safeBack(back);
    }

    // ---------- admins ----------

    @PostMapping("/admin/notices")
    public String post(@RequestParam(defaultValue = "") String title,
                       @RequestParam(defaultValue = "") String body,
                       @RequestParam(defaultValue = "false") boolean showInBanner,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        String cleanTitle = title.trim();
        String cleanBody = body.trim();
        String errorKey = findFirstError(cleanTitle, cleanBody);
        if (errorKey != null) {
            redirect.addFlashAttribute("noticeErrorKey", errorKey);
            redirect.addFlashAttribute("draftTitle", title);
            redirect.addFlashAttribute("draftBody", body);
            return "redirect:/notices";
        }

        SiteNotice notice = new SiteNotice();
        notice.setAuthor(currentUser(authentication));
        notice.setTitle(cleanTitle);
        notice.setBody(cleanBody);
        notice.setShowInBanner(showInBanner);
        notice.setCreatedAt(Instant.now());
        noticeRepository.save(notice);

        notificationService.notifySiteNotice(notice);
        return "redirect:/notices";
    }

    @GetMapping("/admin/notices/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SiteNotice notice = findNotice(id);
        // After a validation error the page shows what the admin typed, not the saved text.
        model.addAttribute("title", model.containsAttribute("draftTitle")
                ? model.getAttribute("draftTitle") : notice.getTitle());
        model.addAttribute("body", model.containsAttribute("draftBody")
                ? model.getAttribute("draftBody") : notice.getBody());
        model.addAttribute("notice", notice);
        return "notice-edit";
    }

    @PostMapping("/admin/notices/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam(defaultValue = "") String title,
                       @RequestParam(defaultValue = "") String body,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        SiteNotice notice = findNotice(id);
        String cleanTitle = title.trim();
        String cleanBody = body.trim();
        String errorKey = findFirstError(cleanTitle, cleanBody);
        if (errorKey != null) {
            redirect.addFlashAttribute("noticeErrorKey", errorKey);
            redirect.addFlashAttribute("draftTitle", title);
            redirect.addFlashAttribute("draftBody", body);
            return "redirect:/admin/notices/" + id + "/edit";
        }

        // Nothing changed: do not mark it as edited and do not tell anyone.
        if (!cleanTitle.equals(notice.getTitle()) || !cleanBody.equals(notice.getBody())) {
            notice.setTitle(cleanTitle);
            notice.setBody(cleanBody);
            notice.setEditedAt(Instant.now());
            noticeRepository.save(notice);
            notificationService.notifySiteNoticeEdited(notice, currentUser(authentication));
        }
        return "redirect:/notices";
    }

    /** Takes the notice off the top bar (for example when the problem is fixed). It stays in the list. */
    @PostMapping("/admin/notices/{id}/banner-off")
    public String bannerOff(@PathVariable Long id) {
        SiteNotice notice = findNotice(id);
        notice.setShowInBanner(false);
        noticeRepository.save(notice);
        return "redirect:/notices";
    }

    /** Puts it on the top bar again (people who closed it before do not see it again). */
    @PostMapping("/admin/notices/{id}/banner-on")
    public String bannerOn(@PathVariable Long id) {
        SiteNotice notice = findNotice(id);
        notice.setShowInBanner(true);
        noticeRepository.save(notice);
        return "redirect:/notices";
    }

    /**
     * Tells everyone about this notice again: an unread notification at the top for every user, and if the
     * notice is in the top bar, the bar comes back for people who had closed it.
     */
    @PostMapping("/admin/notices/{id}/renotify")
    public String renotify(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        SiteNotice notice = findNotice(id);
        notificationService.renotifySiteNotice(notice, currentUser(authentication));
        if (notice.isShowInBanner()) {
            dismissalRepository.deleteByNotice(notice);
        }
        redirect.addFlashAttribute("noticeRenotifiedId", notice.getId());
        return "redirect:/notices";
    }

    @PostMapping("/admin/notices/{id}/delete")
    public String delete(@PathVariable Long id) {
        SiteNotice notice = findNotice(id);
        notice.setDeleted(true);
        notice.setShowInBanner(false);
        noticeRepository.save(notice);
        notificationService.onSiteNoticeDeleted(notice);
        return "redirect:/notices";
    }

    // ---------- helpers ----------

    /** Returns the message key of the first problem, in the same order as the form fields. */
    private String findFirstError(String title, String body) {
        if (title.isEmpty()) {
            return "notices.error.titleRequired";
        }
        if (title.length() > TITLE_MAX) {
            return "notices.error.titleTooLong";
        }
        if (body.isEmpty()) {
            return "notices.error.bodyRequired";
        }
        if (body.length() > BODY_MAX) {
            return "notices.error.bodyTooLong";
        }
        return null;
    }

    /** Only a path inside this app ("/courses/5"), never another site ("//evil.com" or "https://..."). */
    private String safeBack(String back) {
        boolean inside = back.startsWith("/") && !back.startsWith("//") && !back.contains("\\");
        return inside ? back : "/";
    }

    /** A notice that exists and was not deleted, or 404. */
    private SiteNotice findNotice(Long id) {
        return noticeRepository.findById(id)
                .filter(notice -> !notice.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
