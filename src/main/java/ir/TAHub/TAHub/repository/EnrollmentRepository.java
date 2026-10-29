package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentAndOffering(User student, CourseOffering offering);

    // "StudentFullName" means: sort by the full name of the related student.
    List<Enrollment> findByOfferingAndStatusOrderByStudentFullNameAsc(CourseOffering offering,
                                                                      EnrollmentStatus status);
    // All enrollments of one student with the given status (used for "my courses").
    List<Enrollment> findByStudentAndStatus(User student, EnrollmentStatus status);

    long countByOfferingAndStatus(CourseOffering offering, EnrollmentStatus status);

    boolean existsByStudentAndOfferingCourseAndOfferingSemesterAndStatus(
            User student, Course course, Semester semester, EnrollmentStatus status);
}