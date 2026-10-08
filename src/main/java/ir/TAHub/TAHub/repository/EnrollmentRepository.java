package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentAndOffering(User student, CourseOffering offering);

    // "StudentFullName" means: sort by the full name of the related student.
    List<Enrollment> findByOfferingAndStatusOrderByStudentFullNameAsc(CourseOffering offering,
                                                                      EnrollmentStatus status);
}