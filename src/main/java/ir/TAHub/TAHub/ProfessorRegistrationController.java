package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.ProfessorInvite;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.ProfessorInviteRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * A professor creates their own account with a one-time invite code from an admin.
 * Their username is English letters and digits (stored in lower case) and must contain a letter,
 * so it can never look like (or later clash with) a student number.
 */
@Controller
public class ProfessorRegistrationController {

    private final ProfessorInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfessorRegistrationController(ProfessorInviteRepository inviteRepository,
                                           UserRepository userRepository,
                                           PasswordEncoder passwordEncoder) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 4 to 30 English letters and digits, at least one letter. Expects a lower-cased value. */
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("[a-z0-9]{4,30}") && username.matches(".*[a-z].*");
    }

    /** Lower case, no spaces; Persian digits become English ones. */
    public static String cleanUsername(String username) {
        return username == null ? "" : Digits.toAscii(username).trim().toLowerCase(Locale.ROOT);
    }

    /** Invite codes are typed by hand: case, spaces and dashes do not matter. */
    static String cleanCode(String code) {
        return code == null ? "" : Digits.toAscii(code).replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    @GetMapping("/register/professor")
    public String showForm(@RequestParam(defaultValue = "") String code, Model model) {
        model.addAttribute("code", cleanCode(code));
        model.addAttribute("fullName", "");
        model.addAttribute("username", "");
        return "professor-register";
    }

    @PostMapping("/register/professor")
    @Transactional
    public String register(@RequestParam(defaultValue = "") String code,
                           @RequestParam(defaultValue = "") String fullName,
                           @RequestParam(defaultValue = "") String username,
                           @RequestParam(defaultValue = "") String password,
                           @RequestParam(defaultValue = "") String confirmPassword,
                           Model model) {
        String cleanCode = cleanCode(code);
        String name = fullName.trim();
        String cleanName = cleanUsername(username);

        Optional<ProfessorInvite> invite = cleanCode.isEmpty()
                ? Optional.empty()
                : inviteRepository.findByCode(cleanCode).filter(ProfessorInvite::isUsable);

        String errorKey = null;
        if (invite.isEmpty()) {
            errorKey = "profReg.error.code";
        } else if (name.isEmpty()) {
            errorKey = "register.error.fullNameRequired";
        } else if (name.length() > 100) {
            errorKey = "register.error.fullNameTooLong";
        } else if (!isValidUsername(cleanName)) {
            errorKey = "profReg.error.username";
        } else if (userRepository.existsByStudentNumber(cleanName)) {
            errorKey = "profReg.error.usernameTaken";
        } else if (password.isBlank()) {
            errorKey = "register.error.passwordRequired";
        } else if (password.length() < 8 || password.length() > 64) {
            errorKey = "register.error.passwordLength";
        } else if (!password.equals(confirmPassword)) {
            errorKey = "register.error.passwordMismatch";
        }

        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            model.addAttribute("code", cleanCode);
            model.addAttribute("fullName", name);
            model.addAttribute("username", cleanName);
            return "professor-register";
        }

        User professor = new User();
        professor.setFullName(name);
        professor.setStudentNumber(cleanName); // the login name
        professor.setPasswordHash(passwordEncoder.encode(password));
        professor.setRole(Role.PROFESSOR);
        userRepository.save(professor);

        ProfessorInvite used = invite.get();
        used.setUsedAt(Instant.now());
        used.setUsedBy(professor);
        inviteRepository.save(used);

        return "redirect:/login?registered";
    }
}
