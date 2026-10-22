package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

/** The professor (or an admin) adds and removes the teaching assistants of an offering. */
@Controller
public class TaController {

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaAssignmentRepository taRepository;

    public TaController(UserRepository userRepository,
                        CourseOfferingRepository offeringRepository,
                        EnrollmentRepository enrollmentRepository,
                        TaAssignmentRepository taRepository) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taRepository = taRepository;
    }

    @PostMapping("/courses/{id}/tas")
    public String add(@PathVariable Long id,
                      @RequestParam(defaultValue = "") String studentNumber,
                      Authentication authentication,
                      RedirectAttributes redirect) {
        CourseOffering offering = findManagedOffering(id, authentication);

        // Persian digits are accepted too.
        String number = Digits.toAscii(studentNumber).trim();
        User candidate = number.isEmpty() ? null : userRepository.findByStudentNumber(number).orElse(null);

        String errorKey = null;
        if (candidate == null) {
            errorKey = "ta.error.notFound";
        } else if (candidate.getRole() != Role.STUDENT) {
            errorKey = "ta.error.notStudent";
        } else if (isActiveStudentOf(candidate, offering)) {
            // A person cannot be a student and a TA of the same offering.
            errorKey = "ta.error.enrolled";
        }

        if (errorKey != null) {
            redirect.addFlashAttribute("taErrorKey", errorKey);
            return "redirect:/courses/" + id;
        }

        TaAssignment assignment = taRepository.findByUserAndOffering(candidate, offering).orElse(null);
        if (assignment == null) {
            assignment = new TaAssignment();
            assignment.setUser(candidate);
            assignment.setOffering(offering);
            assignment.setAssignedAt(Instant.now());
        } else if (assignment.isActive()) {
            redirect.addFlashAttribute("taErrorKey", "ta.error.already");
            return "redirect:/courses/" + id;
        }
        assignment.setActive(true); // new, or coming back after being removed
        taRepository.save(assignment);
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/tas/{userId}/remove")
    public String remove(@PathVariable Long id,
                         @PathVariable Long userId,
                         Authentication authentication) {
        CourseOffering offering = findManagedOffering(id, authentication);

        userRepository.findById(userId)
                .flatMap(user -> taRepository.findByUserAndOffering(user, offering))
                .filter(TaAssignment::isActive)
                .ifPresent(assignment -> {
                    assignment.setActive(false); // history stays
                    taRepository.save(assignment);
                });
        return "redirect:/courses/" + id;
    }

    private boolean isActiveStudentOf(User user, CourseOffering offering) {
        return enrollmentRepository.findByStudentAndOffering(user, offering)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .isPresent();
    }

    /** Loads an offering of the active semester and checks that the current user manages it. */
    private CourseOffering findManagedOffering(Long id, Authentication authentication) {
        User current = userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!offering.isManagedBy(current) || !offering.getSemester().isActive() || offering.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return offering;
    }
}