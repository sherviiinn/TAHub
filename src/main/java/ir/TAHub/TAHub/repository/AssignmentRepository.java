package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Assignment;
import ir.TAHub.TAHub.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // The professor and TAs: drafts too (newest deadline first).
    List<Assignment> findByOfferingAndDeletedFalseOrderByDeadlineDescIdDesc(CourseOffering offering);

    // Students: only published ones.
    List<Assignment> findByOfferingAndDeletedFalseAndPublishedTrueOrderByDeadlineDescIdDesc(CourseOffering offering);

    long countByOfferingAndDeletedFalse(CourseOffering offering);

    long countByOfferingAndDeletedFalseAndPublishedTrue(CourseOffering offering);
}
