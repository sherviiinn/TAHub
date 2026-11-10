package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Assignment;
import ir.TAHub.TAHub.model.AssignmentExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentExtensionRepository extends JpaRepository<AssignmentExtension, Long> {

    List<AssignmentExtension> findByAssignmentOrderByCreatedAtAscIdAsc(Assignment assignment);

    // The newest extension (its factor is the one shown next to the deadline).
    Optional<AssignmentExtension> findFirstByAssignmentOrderByCreatedAtDescIdDesc(Assignment assignment);
}
