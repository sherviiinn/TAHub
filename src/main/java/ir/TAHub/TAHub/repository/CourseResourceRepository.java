package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.CourseResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseResourceRepository extends JpaRepository<CourseResource, Long> {

    // Oldest first; the page groups them by category.
    List<CourseResource> findByOfferingAndDeletedFalseOrderByCreatedAtAscIdAsc(CourseOffering offering);

    // The resources of several offerings (all groups of the course in the last semester), for suggestions.
    List<CourseResource> findByOfferingInAndDeletedFalseOrderByCreatedAtAscIdAsc(Collection<CourseOffering> offerings);

    long countByOfferingAndDeletedFalse(CourseOffering offering);
}
