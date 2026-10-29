package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.OfferingAccess;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The Telegram group link of an offering.
 * The professor (and admins) and TAs with "Edit Telegram link" set, change or remove it, only in an
 * uncancelled offering of the active semester. The link is shown to the same people who can read the announcements
 * (the professor, the TAs and the enrolled students), because group links are usually private.
 */
@Controller
public class TelegramLinkController {

    private static final int LINK_MAX = 200;

    // Only real Telegram links, so nobody can put a link to another (fake) site here.
    // Examples: https://t.me/+AbC123, https://t.me/joinchat/AbC123, https://t.me/some_group
    private static final Pattern TELEGRAM_LINK =
            Pattern.compile("^https://(t\\.me|telegram\\.me)/[A-Za-z0-9_+\\-/]+$");

    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OfferingAccess access;

    public TelegramLinkController(CourseOfferingRepository offeringRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  OfferingAccess access) {
        this.offeringRepository = offeringRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.access = access;
    }

    /** Saves the link. An empty box removes it. */
    @PostMapping("/courses/{id}/telegram")
    public String save(@PathVariable Long id,
                       @RequestParam(defaultValue = "") String link,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!access.isOpen(offering) || !access.can(current, offering, TaPermission.EDIT_TELEGRAM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String clean = link.trim();
        if (!clean.isEmpty()) {
            if (clean.length() > LINK_MAX) {
                redirect.addFlashAttribute("telegramErrorKey", "telegram.error.tooLong");
                return "redirect:/courses/" + id;
            }
            if (!TELEGRAM_LINK.matcher(clean).matches()) {
                redirect.addFlashAttribute("telegramErrorKey", "telegram.error.notTelegram");
                return "redirect:/courses/" + id;
            }
        }

        String newLink = clean.isEmpty() ? null : clean;
        if (!Objects.equals(newLink, offering.getTelegramLink())) {
            offering.setTelegramLink(newLink);
            offeringRepository.save(offering);
            // Students are told about a new or changed link (not about a removed one).
            if (newLink != null) {
                notificationService.notifyTelegramLink(offering);
            }
        }
        redirect.addFlashAttribute("telegramSaved", true);
        return "redirect:/courses/" + id;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
