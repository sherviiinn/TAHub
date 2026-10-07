package ir.TAHub.TAHub;

import ir.TAHub.TAHub.dto.RegisterForm;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult result) {

        // Checks that annotations cannot express.
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "register.error.mismatch", "Passwords do not match.");
        }
        if (form.getStudentNumber() != null
                && userRepository.existsByStudentNumber(form.getStudentNumber())) {
            result.rejectValue("studentNumber", "register.error.duplicate",
                    "This student number is already registered.");
        }

        // If anything failed, show the same page again with the error messages.
        if (result.hasErrors()) {
            return "register";
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setStudentNumber(form.getStudentNumber());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.STUDENT); // never taken from user input
        userRepository.save(user);

        return "redirect:/login?registered";
    }
}