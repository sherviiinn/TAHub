package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        // The login username is the student number, so we use it to load the full user.
        Optional<User> found = userRepository.findByStudentNumber(authentication.getName());

        // The account may have been deleted while the user was still logged in.
        if (found.isEmpty()) {
            return "redirect:/login";
        }

        User user = found.get();
        model.addAttribute("appName", "TA Hub");
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("role", user.getRole());
        model.addAttribute("isAdmin", user.getRole() == Role.ADMIN);
        return "home";
    }
}