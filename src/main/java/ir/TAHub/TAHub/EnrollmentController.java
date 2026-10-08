package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
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

/** Enrolling in, dropping, removing and restoring students in course offerings. */
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

        Enrollment enrollment = enrollmentRepository.findByStudentAndOffering(student, offering).orElse(null);

        if (enrollment == null) {
            enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setOffering(offering);
            enrollment.setEnrolledAt(Instant.now());
            changeStatus(enrollment, EnrollmentStatus.ACTIVE);
        } else if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            // The student left earlier and now comes back: reuse the same row.
            changeStatus(enrollment, EnrollmentStatus.ACTIVE);
        } else if (enrollment.getStatus() == EnrollmentStatus.REMOVED) {
            // A student removed by the professor cannot join again by themselves.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // If the status is already ACTIVE there is nothing to do.

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
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .ifPresent(enrollment -> changeStatus(enrollment, EnrollmentStatus.DROPPED));
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/students/{studentId}/remove")
    public String remove(@PathVariable Long id,
                         @PathVariable Long studentId,
                         Authentication authentication) {
        CourseOffering offering = findManagedOffering(id, authentication);

        findEnrollment(studentId, offering)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .ifPresent(enrollment -> changeStatus(enrollment, EnrollmentStatus.REMOVED));
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/students/{studentId}/restore")
    public String restore(@PathVariable Long id,
                          @PathVariable Long studentId,
                          Authentication authentication) {
        CourseOffering offering = findManagedOffering(id, authentication);

        findEnrollment(studentId, offering)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.REMOVED)
                .ifPresent(enrollment -> changeStatus(enrollment, EnrollmentStatus.ACTIVE));
        return "redirect:/courses/" + id;
    }

    /**
     * Loads the offering and checks that the current user may manage it.
     * URL rules cannot know who owns an offering, so we check it here.
     */
    private CourseOffering findManagedOffering(Long id, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOffering(id);
        if (!offering.isManagedBy(current) || !offering.getSemester().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return offering;
    }

    private java.util.Optional<Enrollment> findEnrollment(Long studentId, CourseOffering offering) {
        return userRepository.findById(studentId)
                .flatMap(student -> enrollmentRepository.findByStudentAndOffering(student, offering));
    }

    private void changeStatus(Enrollment enrollment, EnrollmentStatus status) {
        enrollment.setStatus(status);
        enrollmentRepository.save(enrollment);
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