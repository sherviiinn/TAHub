package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.ProfessorInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessorInviteRepository extends JpaRepository<ProfessorInvite, Long> {

    Optional<ProfessorInvite> findByCode(String code);

    boolean existsByCode(String code);

    List<ProfessorInvite> findTop100ByOrderByCreatedAtDesc();
}
