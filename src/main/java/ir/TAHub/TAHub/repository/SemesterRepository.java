package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByActiveTrue();

    boolean existsByCode(String code);

    List<Semester> findAllByOrderByIdDesc();
}