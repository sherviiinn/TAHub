package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaAssignmentRepository extends JpaRepository<TaAssignment, Long> {

    Optional<TaAssignment> findByUserAndOffering(User user, CourseOffering offering);

    boolean existsByUserAndOfferingAndActiveTrue(User user, CourseOffering offering);

    List<TaAssignment> findByOfferingAndActiveTrueOrderByUserFullNameAsc(CourseOffering offering);

    // TAs the professor removed. They stay listed so the professor can bring them back.
    List<TaAssignment> findByOfferingAndActiveFalseOrderByUserFullNameAsc(CourseOffering offering);
}