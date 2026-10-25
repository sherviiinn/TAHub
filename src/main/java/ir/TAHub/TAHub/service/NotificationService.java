package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.*;
import ir.TAHub.TAHub.repository.AnnouncementRepository;
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
    private final AnnouncementRepository announcementRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               EnrollmentRepository enrollmentRepository,
                               AnnouncementRepository announcementRepository) {
        this.notificationRepository = notificationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.announcementRepository = announcementRepository;
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
                Notification created = create(student, NotificationType.ANNOUNCEMENT, title, link);
                created.setFirstEventAt(announcement.getCreatedAt());
                notifications.add(created);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * Call this after an announcement was edited. Every enrolled student (except the author) is told,
     * once per announcement: if they have not read the earlier "edited" notification, it just moves to the top.
     * A student who has an unread "new announcement" notification that already covers this announcement
     * is not told again, because they will see the new text anyway when they open it.
     */
    @Transactional
    public void notifyAnnouncementEdited(Announcement announcement) {
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
            if (coveredByUnreadNewNotification(student, link, announcement)) {
                continue;
            }
            Notification existing = notificationRepository
                    .findFirstByUserAndAnnouncementIdAndSeenFalse(student, announcement.getId())
                    .orElse(null);
            if (existing != null) {
                existing.setCreatedAt(Instant.now());
                notifications.add(existing);
            } else {
                Notification created = create(student, NotificationType.GENERAL, title, link);
                created.setAnnouncementId(announcement.getId());
                notifications.add(created);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    /** True if the student has an unread "new announcement" notification whose time window includes it. */
    private boolean coveredByUnreadNewNotification(User student, String link, Announcement announcement) {
        Notification unread = notificationRepository
                .findFirstByUserAndTypeAndLinkAndSeenFalse(student, NotificationType.ANNOUNCEMENT, link)
                .orElse(null);
        if (unread == null) {
            return false;
        }
        Instant from = unread.getFirstEventAt() != null ? unread.getFirstEventAt() : unread.getCreatedAt();
        Instant created = announcement.getCreatedAt();
        return !created.isBefore(from) && !created.isAfter(unread.getCreatedAt());
    }

    /**
     * Call this after an announcement was deleted. It fixes the notifications of every student of the course,
     * read or unread. A notification covers the announcements from firstEventAt up to createdAt
     * (createdAt moves forward each time a new announcement is added). If no announcement is left in that
     * window the notification is deleted completely, otherwise its counter is lowered and its time goes back
     * to the newest announcement that is left (so the order of the list is the same as before the deleted one).
     */
    @Transactional
    public void onAnnouncementDeleted(Announcement announcement) {
        CourseOffering offering = announcement.getOffering();
        String link = "/courses/" + offering.getId() + "/announcements";

        // "This announcement was edited" notifications are simply removed, read or unread.
        notificationRepository.deleteAll(notificationRepository.findByAnnouncementId(announcement.getId()));

        for (Notification notification : notificationRepository
                .findByTypeAndLink(NotificationType.ANNOUNCEMENT, link)) {
            Instant from = notification.getFirstEventAt() != null
                    ? notification.getFirstEventAt()
                    : notification.getCreatedAt();
            Instant to = notification.getCreatedAt();
            // The deleted announcement was not part of this notification.
            if (announcement.getCreatedAt().isBefore(from) || announcement.getCreatedAt().isAfter(to)) {
                continue;
            }

            List<Announcement> left = announcementRepository
                    .findByOfferingAndDeletedFalseAndCreatedAtBetweenAndAuthorNotOrderByCreatedAtDescIdDesc(
                            offering, from, to, notification.getUser());
            if (left.isEmpty()) {
                notificationRepository.delete(notification);
            } else {
                // The notification goes back to the place it had before the deleted announcement:
                // it now belongs to the newest announcement that is still there.
                notification.setItemCount(left.size());
                notification.setCreatedAt(left.get(0).getCreatedAt());
                notification.setFirstEventAt(left.get(left.size() - 1).getCreatedAt());
                notificationRepository.save(notification);
            }
        }
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
