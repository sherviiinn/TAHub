package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.Notification;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // The newest 50 are enough for the page. Older ones stay in the database.
    List<Notification> findTop50ByUserOrderByCreatedAtDescIdDesc(User user);

    long countByUserAndSeenFalse(User user);

    List<Notification> findByUserAndSeenFalse(User user);
}
