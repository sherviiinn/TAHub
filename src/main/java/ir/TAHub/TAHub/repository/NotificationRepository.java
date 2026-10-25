package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Notification;
import ir.TAHub.TAHub.model.NotificationType;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // The newest 50 are enough for the page. Older ones stay in the database.
    List<Notification> findTop50ByUserOrderByCreatedAtDescIdDesc(User user);

    // An unread notification of the same kind and place, so we can add to it instead of making a new one.
    Optional<Notification> findFirstByUserAndTypeAndLinkAndSeenFalse(User user, NotificationType type, String link);

    // All notifications (read or unread) of one kind and place (every student of one course).
    List<Notification> findByTypeAndLink(NotificationType type, String link);

    long countByUserAndSeenFalse(User user);

    List<Notification> findByUserAndSeenFalse(User user);
}
