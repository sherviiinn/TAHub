package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the data that the shared menu (templates/fragments/layout.html) needs to EVERY page,
 * so no controller has to add it by hand. All names start with "nav".
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addNavigationData(Model model, Authentication authentication) {
        // Not logged in (login and register pages): no menu.
        User user = authentication == null
                ? null
                : userRepository.findByStudentNumber(authentication.getName()).orElse(null);

        model.addAttribute("navLoggedIn", user != null);
        if (user != null) {
            model.addAttribute("navFullName", user.getFullName());
            model.addAttribute("navIsStudent", user.getRole() == Role.STUDENT);
            model.addAttribute("navIsProfessor", user.getRole() == Role.PROFESSOR);
            model.addAttribute("navIsAdmin", user.getRole() == Role.ADMIN);
        }
    }
}
