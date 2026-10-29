package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Course;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaAssignmentRepository extends JpaRepository<TaAssignment, Long> {

    Optional<TaAssignment> findByUserAndOffering(User user, CourseOffering offering);

    boolean existsByUserAndOfferingAndActiveTrue(User user, CourseOffering offering);

    // Is this user an active TA of ANY group (offering) of this course in this semester?
    boolean existsByUserAndOfferingCourseAndOfferingSemesterAndActiveTrue(User user, Course course, Semester semester);

    List<TaAssignment> findByOfferingAndActiveTrueOrderByUserFullNameAsc(CourseOffering offering);

    // TAs that were removed (the rows stay for history).
    List<TaAssignment> findByOfferingAndActiveFalseOrderByUserFullNameAsc(CourseOffering offering);
}
