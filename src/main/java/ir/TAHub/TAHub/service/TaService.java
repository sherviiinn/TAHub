package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * The rules for making someone a TA. Used when a TA is added by hand and when a request is accepted.
 * A course can have several groups (offerings) in one semester. A person cannot be a student of one group
 * and a TA of another group of the same course. Being a TA of two groups is fine.
 */
@Service
public class TaService {

    private final EnrollmentRepository enrollmentRepository;
    private final TaAssignmentRepository taRepository;

    public TaService(EnrollmentRepository enrollmentRepository, TaAssignmentRepository taRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.taRepository = taRepository;
    }

    /** Returns the message key of the first problem, or null if this person can become a TA of the offering. */
    public String findProblem(User candidate, CourseOffering offering) {
        if (candidate.getRole() != Role.STUDENT) {
            return "ta.error.notStudent";
        }
        // A person cannot be a student and a TA of the same offering.
        boolean activeStudent = enrollmentRepository.findByStudentAndOffering(candidate, offering)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .isPresent();
        if (activeStudent) {
            return "ta.error.enrolled";
        }
        // Nor a student of another group of the same course in the same semester.
        if (isStudentOfAnyGroup(candidate, offering)) {
            return "ta.error.otherGroup";
        }
        if (taRepository.existsByUserAndOfferingAndActiveTrue(candidate, offering)) {
            return "ta.error.already";
        }
        return null;
    }

    /** Is this person an active student of any group (offering) of this course in this semester? */
    public boolean isStudentOfAnyGroup(User user, CourseOffering offering) {
        return enrollmentRepository.existsByStudentAndOfferingCourseAndOfferingSemesterAndStatus(
                user, offering.getCourse(), offering.getSemester(), EnrollmentStatus.ACTIVE);
    }

    /** Is this person an active TA of any group (offering) of this course in this semester? */
    public boolean isTaOfAnyGroup(User user, CourseOffering offering) {
        return taRepository.existsByUserAndOfferingCourseAndOfferingSemesterAndActiveTrue(
                user, offering.getCourse(), offering.getSemester());
    }

    /**
     * Makes the person a TA (or activates their old assignment) with these permissions.
     * Returns a problem key, or null on success.
     */
    @Transactional
    public String assign(User candidate, CourseOffering offering, Set<TaPermission> permissions) {
        String problem = findProblem(candidate, offering);
        if (problem != null) {
            return problem;
        }
        TaAssignment assignment = taRepository.findByUserAndOffering(candidate, offering).orElse(null);
        if (assignment == null) {
            assignment = new TaAssignment();
            assignment.setUser(candidate);
            assignment.setOffering(offering);
        }
        assignment.setAssignedAt(Instant.now());
        assignment.setActive(true); // new, or coming back after being removed
        assignment.replacePermissions(permissions); // a returning TA does not keep old permissions
        taRepository.save(assignment);
        return null;
    }

    /** Same, with the default permissions (used when the professor makes someone a TA). */
    @Transactional
    public String assign(User candidate, CourseOffering offering) {
        return assign(candidate, offering, TaPermission.defaults());
    }
}
