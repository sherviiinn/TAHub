package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.StudentNumberRules;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin page for adding users by hand. There is no "delete" here any more: disabling an account or
 * releasing a student number is done carefully in the accounts panel (/admin/accounts).
 */
@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentNumberRules numberRules;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          StudentNumberRules numberRules) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.numberRules = numberRules;
    }

    @GetMapping("/users")
    public String list(Model model) {
        fillModel(model);
        return "users";
    }

    @PostMapping("/users")
    public String add(@RequestParam(defaultValue = "") String fullName,
                      @RequestParam(defaultValue = "") String studentNumber,
                      @RequestParam(defaultValue = "") String password,
                      @RequestParam Role role,
                      Model model) {
        String name = fullName.trim();
        // Students log in with their number; professors and admins with a username (lower case).
        String login = role == Role.STUDENT
                ? numberRules.clean(studentNumber)
                : ProfessorRegistrationController.cleanUsername(studentNumber);

        String errorKey = null;
        if (name.isEmpty()) {
            errorKey = "register.error.fullNameRequired";
        } else if (name.length() > 100) {
            errorKey = "register.error.fullNameTooLong";
        } else if (role == Role.STUDENT && !numberRules.isValid(login)) {
            errorKey = "register.error.studentNumberFormat";
        } else if (role != Role.STUDENT && !ProfessorRegistrationController.isValidUsername(login)) {
            errorKey = "profReg.error.username";
        } else if (userRepository.existsByStudentNumber(login)) {
            errorKey = "users.error.taken";
        } else if (password.length() < 8 || password.length() > 64) {
            errorKey = "register.error.passwordLength";
        }

        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            fillModel(model);
            return "users";
        }

        User user = new User();
        user.setFullName(name);
        user.setStudentNumber(login);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        return "redirect:/users";
    }

    private void fillModel(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("hint", numberRules.hint());
    }
}
