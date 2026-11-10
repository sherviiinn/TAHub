package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Major;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.MajorRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * The logged-in user's own profile.
 * A student chooses the field of study only ONCE, and only if they have none (for example an account an admin
 * made). After that only an admin changes it: later through a change request with a photo of the student card
 * (after step 4-p, when file uploads exist), until then from the accounts panel (/admin/accounts).
 */
@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final MajorRepository majorRepository;

    public ProfileController(UserRepository userRepository, MajorRepository majorRepository) {
        this.userRepository = userRepository;
        this.majorRepository = majorRepository;
    }

    @GetMapping("/profile")
    public String show(Model model, Authentication authentication) {
        User current = currentUser(authentication);
        boolean isStudent = current.getRole() == Role.STUDENT;
        model.addAttribute("profileUser", current);
        model.addAttribute("isStudent", isStudent);
        // The page shows the form only when this is true; otherwise the "request a change" card.
        model.addAttribute("canChangeMajor", isStudent && current.getMajor() == null);
        model.addAttribute("majors", majorRepository.findAllByOrderByNameAsc());
        return "profile";
    }

    @PostMapping("/profile/major")
    public String chooseMajor(@RequestParam Long majorId, Authentication authentication) {
        User current = currentUser(authentication);

        // Only students have a field of study.
        if (current.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // Already set: the student cannot change it alone, even when the request is sent without the page.
        if (current.getMajor() != null) {
            return "redirect:/profile?majorLocked";
        }
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

        // We change the logged-in user, never a user chosen by an id from the request.
        current.setMajor(major);
        userRepository.save(current);
        return "redirect:/profile?saved";
    }

    /** The page behind "Request a change". For now it explains the process; the real form comes after step 4-p. */
    @GetMapping("/profile/major-request")
    public String majorRequest(Model model, Authentication authentication) {
        User current = currentUser(authentication);
        if (current.getRole() != Role.STUDENT) {
            return "redirect:/profile";
        }
        model.addAttribute("profileUser", current);
        return "major-request";
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
