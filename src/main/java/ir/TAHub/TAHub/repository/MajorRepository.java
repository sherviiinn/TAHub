package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MajorRepository extends JpaRepository<Major, Long> {

    List<Major> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}