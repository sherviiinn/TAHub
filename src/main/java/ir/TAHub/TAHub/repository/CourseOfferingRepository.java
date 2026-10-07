package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    // Newest semester first, then by course code.
    List<CourseOffering> findAllByOrderBySemesterDescCodeAsc();
}