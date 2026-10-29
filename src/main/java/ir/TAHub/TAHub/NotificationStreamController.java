package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationStream;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The live connection used by static/js/live.js. Only for logged-in users (like every page except
 * login and register). The browser receives "unread" events with the current unread count.
 */
@Controller
public class NotificationStreamController {

    private final NotificationStream stream;
    private final UserRepository userRepository;

    public NotificationStreamController(NotificationStream stream, UserRepository userRepository) {
        this.stream = stream;
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(Authentication authentication) {
        User current = userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
        return stream.connect(current);
    }
}
