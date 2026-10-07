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
    public String register(@ModelAttribute("form") RegisterForm form, Model model) {

        // Clean up whitespace first, so " 12345 " is treated as "12345".
        form.setFullName(trimOrEmpty(form.getFullName()));
        form.setStudentNumber(trimOrEmpty(form.getStudentNumber()));

        String errorKey = findFirstError(form);
        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            return "register";
        }

        User user = new User();
        user.setFullName(form.getFullName());
        user.setStudentNumber(form.getStudentNumber());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.STUDENT); // never taken from user input
        userRepository.save(user);

        return "redirect:/login?registered";
    }

    /**
     * Checks the form in the same order as the fields on the page and
     * returns the message key of the FIRST problem, or null if all is fine.
     */
    private String findFirstError(RegisterForm form) {
        if (form.getFullName().isEmpty()) {
            return "register.error.fullNameRequired";
        }
        if (form.getFullName().length() > 100) {
            return "register.error.fullNameTooLong";
        }
        if (form.getStudentNumber().isEmpty()) {
            return "register.error.studentNumberRequired";
        }
        if (!form.getStudentNumber().matches("\\d{4,20}")) {
            return "register.error.studentNumberFormat";
        }
        if (userRepository.existsByStudentNumber(form.getStudentNumber())) {
            return "register.error.studentNumberTaken";
        }
        String password = form.getPassword();
        if (password == null || password.isBlank()) {
            return "register.error.passwordRequired";
        }
        if (password.length() < 8 || password.length() > 64) {
            return "register.error.passwordLength";
        }
        if (!password.equals(form.getConfirmPassword())) {
            return "register.error.passwordMismatch";
        }
        return null;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}