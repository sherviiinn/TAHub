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

/** The logged-in user's own profile. For now a student can view it and change the field of study. */
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
        model.addAttribute("profileUser", current);
        model.addAttribute("isStudent", current.getRole() == Role.STUDENT);
        model.addAttribute("majors", majorRepository.findAllByOrderByNameAsc());
        return "profile";
    }

    @PostMapping("/profile/major")
    public String changeMajor(@RequestParam Long majorId, Authentication authentication) {
        User current = currentUser(authentication);

        // Only students have a field of study.
        if (current.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

        // We change the logged-in user, never a user chosen by an id from the request.
        current.setMajor(major);
        userRepository.save(current);
        return "redirect:/profile?saved";
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}