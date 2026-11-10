package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Major;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.MajorRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.AccountService;
import ir.TAHub.TAHub.service.StudentNumberRules;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin panel for accounts (/admin/** is admin-only, see SecurityConfig). Nothing here deletes anything.
 * Step 1 "Disable" can be undone. Step 2 "Release number" is only for disabled student accounts and the
 * admin must type the student number again, so it can never happen by one accidental click.
 */
@Controller
public class AccountController {

    private static final int MAX_ROWS = 100;
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Tehran"));

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaAssignmentRepository taRepository;
    private final AccountService accountService;
    private final StudentNumberRules numberRules;
    private final MajorRepository majorRepository;

    public AccountController(UserRepository userRepository,
                             EnrollmentRepository enrollmentRepository,
                             TaAssignmentRepository taRepository,
                             AccountService accountService,
                             StudentNumberRules numberRules,
                             MajorRepository majorRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taRepository = taRepository;
        this.accountService = accountService;
        this.numberRules = numberRules;
        this.majorRepository = majorRepository;
    }

    @GetMapping("/admin/accounts")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "false") boolean invalid,
                       Model model) {
        String query = numberRules.clean(q);
        List<User> users;
        if (invalid) {
            // Student accounts (not released) whose number does not follow the current format.
            users = userRepository.findByRoleAndReleasedNumberIsNullOrderByFullNameAsc(Role.STUDENT).stream()
                    .filter(user -> !numberRules.isValid(user.getStudentNumber()))
                    .limit(MAX_ROWS)
                    .toList();
        } else if (!query.isEmpty()) {
            users = userRepository.search(query, PageRequest.of(0, MAX_ROWS));
        } else {
            users = List.of();
        }

        Map<Long, Long> courses = new HashMap<>();
        Map<Long, Long> taRoles = new HashMap<>();
        Map<Long, String> disabledAt = new HashMap<>();
        Map<Long, Boolean> validNumber = new HashMap<>();
        for (User user : users) {
            courses.put(user.getId(), enrollmentRepository.countByStudentAndStatus(user, EnrollmentStatus.ACTIVE));
            taRoles.put(user.getId(), taRepository.countByUserAndActiveTrue(user));
            if (user.getDisabledAt() != null) {
                disabledAt.put(user.getId(), WHEN.format(user.getDisabledAt()));
            }
            validNumber.put(user.getId(), user.getRole() != Role.STUDENT || numberRules.isValid(user.getStudentNumber()));
        }

        model.addAttribute("q", q);
        model.addAttribute("invalid", invalid);
        model.addAttribute("searched", invalid || !query.isEmpty());
        model.addAttribute("users", users);
        model.addAttribute("courses", courses);
        model.addAttribute("taRoles", taRoles);
        model.addAttribute("disabledAt", disabledAt);
        model.addAttribute("validNumber", validNumber);
        model.addAttribute("settings", numberRules.settings());
        model.addAttribute("hint", numberRules.hint());
        model.addAttribute("majors", majorRepository.findAllByOrderByNameAsc());
        return "accounts";
    }

    @PostMapping("/admin/accounts/{id}/disable")
    public String disable(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String q,
                          Authentication authentication,
                          RedirectAttributes flash) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return back(q, flash, "accounts.error.notFound", null);
        }
        User user = found.get();
        if (user.getStudentNumber().equals(authentication.getName())) {
            return back(q, flash, "accounts.error.self", null);
        }
        accountService.disable(user);
        return back(q, flash, null, "accounts.done.disabled");
    }

    @PostMapping("/admin/accounts/{id}/enable")
    public String enable(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String q,
                         RedirectAttributes flash) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return back(q, flash, "accounts.error.notFound", null);
        }
        if (found.get().getReleasedNumber() != null) {
            return back(q, flash, "accounts.error.released", null);
        }
        accountService.enable(found.get());
        return back(q, flash, null, "accounts.done.enabled");
    }

    @PostMapping("/admin/accounts/{id}/release")
    public String release(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "") String confirmNumber,
                          RedirectAttributes flash) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return back(q, flash, "accounts.error.notFound", null);
        }
        User user = found.get();
        if (user.getRole() != Role.STUDENT) {
            return back(q, flash, "accounts.error.notStudent", null);
        }
        if (user.getReleasedNumber() != null) {
            return back(q, flash, "accounts.error.released", null);
        }
        if (user.getDisabledAt() == null) {
            return back(q, flash, "accounts.error.disableFirst", null);
        }
        if (!numberRules.clean(confirmNumber).equals(user.getStudentNumber())) {
            return back(q, flash, "accounts.error.confirm", null);
        }
        String number = user.getStudentNumber();
        accountService.release(user);
        // Search for the released number, so the admin sees the result (it is kept in releasedNumber).
        return back(number, flash, null, "accounts.done.released");
    }

    /**
     * Students cannot change their own field of study once it is set. Until the change request with a student
     * card exists (after step 4-p), an admin changes it here.
     */
    @PostMapping("/admin/accounts/{id}/major")
    public String changeMajor(@PathVariable Long id,
                              @RequestParam(required = false) Long majorId,
                              @RequestParam(defaultValue = "") String q,
                              RedirectAttributes flash) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return back(q, flash, "accounts.error.notFound", null);
        }
        User user = found.get();
        if (user.getRole() != Role.STUDENT) {
            return back(q, flash, "accounts.error.majorNotStudent", null);
        }
        Optional<Major> major = majorId == null ? Optional.empty() : majorRepository.findById(majorId);
        if (major.isEmpty()) {
            return back(q, flash, "accounts.error.majorNotFound", null);
        }
        user.setMajor(major.get());
        userRepository.save(user);
        return back(q, flash, null, "accounts.done.majorChanged");
    }

    @PostMapping("/admin/accounts/number-format")
    public String saveFormat(@RequestParam(defaultValue = "0") int digits,
                             @RequestParam(defaultValue = "-1") int yearsBack,
                             RedirectAttributes flash) {
        if (digits < 4 || digits > 20) {
            return back("", flash, "accounts.format.error.digits", null);
        }
        if (yearsBack < 0 || yearsBack > 50) {
            return back("", flash, "accounts.format.error.years", null);
        }
        numberRules.save(digits, yearsBack);
        return back("", flash, null, "accounts.format.saved");
    }

    private String back(String q, RedirectAttributes flash, String errorKey, String doneKey) {
        if (errorKey != null) {
            flash.addFlashAttribute("accountErrorKey", errorKey);
        }
        if (doneKey != null) {
            flash.addFlashAttribute("accountDoneKey", doneKey);
        }
        if (q == null || q.isBlank()) {
            return "redirect:/admin/accounts";
        }
        return "redirect:" + UriComponentsBuilder.fromPath("/admin/accounts").queryParam("q", q).encode().toUriString();
    }
}
