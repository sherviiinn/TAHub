package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/** Enrolling in, dropping and removing students from course offerings. */
@Controller
public class EnrollmentController {

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentController(UserRepository userRepository,
                                CourseOfferingRepository offeringRepository,
                                EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @PostMapping("/courses/{id}/enroll")
    public String enroll(@PathVariable Long id, Authentication authentication) {
        User student = currentUser(authentication);
        CourseOffering offering = findOffering(id);

        // Only students can enroll, and only in the active semester.
        if (student.getRole() != Role.STUDENT || !offering.getSemester().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Enrolling twice does nothing (the database also forbids duplicates).
        if (!enrollmentRepository.existsByStudentAndOffering(student, offering)) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setOffering(offering);
            enrollment.setEnrolledAt(Instant.now());
            enrollmentRepository.save(enrollment);
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/drop")
    public String drop(@PathVariable Long id, Authentication authentication) {
        User student = currentUser(authentication);
        CourseOffering offering = findOffering(id);

        if (!offering.getSemester().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // A user can only drop their own enrollment: we look it up using the logged-in user.
        enrollmentRepository.findByStudentAndOffering(student, offering)
                .ifPresent(enrollmentRepository::delete);
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/students/{studentId}/remove")
    public String remove(@PathVariable Long id,
                         @PathVariable Long studentId,
                         Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);

        // URL rules cannot know who owns this offering, so we check it here.
        if (!offering.isManagedBy(current) || !offering.getSemester().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        userRepository.findById(studentId)
                .flatMap(student -> enrollmentRepository.findByStudentAndOffering(student, offering))
                .ifPresent(enrollmentRepository::delete);
        return "redirect:/courses/" + id;
    }

    private CourseOffering findOffering(Long id) {
        return offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}