package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Newest first. The id breaks ties between announcements posted in the same moment.
    List<Announcement> findByOfferingAndDeletedFalseOrderByCreatedAtDescIdDesc(CourseOffering offering);

    List<Announcement> findByOfferingAndDeletedTrueOrderByCreatedAtDescIdDesc(CourseOffering offering);

    long countByOfferingAndDeletedFalse(CourseOffering offering);
}
