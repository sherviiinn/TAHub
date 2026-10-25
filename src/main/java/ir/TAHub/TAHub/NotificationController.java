package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Notification;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.NotificationRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** The notifications page of the logged-in user. */
@Controller
public class NotificationController {

    // Users are in Iran, so times are shown in Tehran time.
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Tehran"));

    /** One notification as the page shows it. */
    public record NotificationView(Notification notification, String when) {
    }

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public NotificationController(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/notifications")
    public String list(Model model, Authentication authentication) {
        User current = currentUser(authentication);
        List<NotificationView> notifications = notificationRepository
                .findTop50ByUserOrderByCreatedAtDescIdDesc(current).stream()
                .map(n -> new NotificationView(n, WHEN.format(n.getCreatedAt())))
                .toList();

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationRepository.countByUserAndSeenFalse(current));
        return "notifications";
    }

    /** Marks one notification as seen and goes to the page it points to. */
    @PostMapping("/notifications/{id}/open")
    public String open(@PathVariable Long id, Authentication authentication) {
        User current = currentUser(authentication);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Nobody can open (or mark) another person's notification. We compare ids, not names.
        if (!notification.getUser().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!notification.isSeen()) {
            notification.setSeen(true);
            notificationRepository.save(notification);
        }
        return "redirect:" + notification.getLink();
    }

    @PostMapping("/notifications/read-all")
    public String readAll(Authentication authentication) {
        User current = currentUser(authentication);
        List<Notification> unseen = notificationRepository.findByUserAndSeenFalse(current);
        unseen.forEach(notification -> notification.setSeen(true));
        notificationRepository.saveAll(unseen);
        return "redirect:/notifications";
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
