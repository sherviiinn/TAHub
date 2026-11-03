package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.ProfessorProfile;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorProfileRepository extends JpaRepository<ProfessorProfile, Long> {

    Optional<ProfessorProfile> findByProfessor(User professor);
}
