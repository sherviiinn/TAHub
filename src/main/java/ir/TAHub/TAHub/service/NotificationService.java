package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.NotificationType;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Creates notifications. Other parts of the app call this when something happens. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               EnrollmentRepository enrollmentRepository) {
        this.notificationRepository = notificationRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Every enrolled student of the offering (except the author, if the author is a student) is told.
     * To keep the notifications page short, a student gets ONE unread notification per course:
     * a new announcement is added to it (its counter grows) instead of creating another row.
     */
    @Transactional
    public void notifyAnnouncement(Announcement announcement) {
        CourseOffering offering = announcement.getOffering();
        String title = offering.getCourse().getCode() + " - " + offering.getCourse().getName();
        String link = "/courses/" + offering.getId() + "/announcements";

        List<Notification> notifications = new ArrayList<>();
        for (Enrollment enrollment : enrollmentRepository
                .findByOfferingAndStatusOrderByStudentFullNameAsc(offering, EnrollmentStatus.ACTIVE)) {
            User student = enrollment.getStudent();
            if (student.getId().equals(announcement.getAuthor().getId())) {
                continue;
            }
            Notification existing = notificationRepository
                    .findFirstByUserAndTypeAndLinkAndSeenFalse(student, NotificationType.ANNOUNCEMENT, link)
                    .orElse(null);
            if (existing != null) {
                existing.setItemCount(existing.getItemCount() + 1);
                existing.setCreatedAt(Instant.now()); // it moves to the top of the list again
                notifications.add(existing);
            } else {
                notifications.add(create(student, NotificationType.ANNOUNCEMENT, title, link));
            }
        }
        notificationRepository.saveAll(notifications);
    }

    /** Builds a notification (it is saved by the caller). */
    public Notification create(User user, NotificationType type, String title, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setLink(link);
        notification.setItemCount(1);
        notification.setSeen(false);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
