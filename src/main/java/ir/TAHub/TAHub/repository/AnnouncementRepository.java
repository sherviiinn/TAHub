package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Newest first. The id breaks ties between announcements posted in the same moment.
    List<Announcement> findByOfferingAndDeletedFalseOrderByCreatedAtDescIdDesc(CourseOffering offering);

    long countByOfferingAndDeletedFalse(CourseOffering offering);

        // Announcements a notification still covers: not deleted, inside a time window, not written by the student.
    // Newest first, so the first element gives the time the notification should now have.
    List<Announcement> findByOfferingAndDeletedFalseAndCreatedAtBetweenAndAuthorNotOrderByCreatedAtDescIdDesc(
            CourseOffering offering, Instant from, Instant to, User author);
}
