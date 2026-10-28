package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Notification;
import ir.TAHub.TAHub.model.NotificationType;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.AnnouncementRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.NotificationRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Creates notifications. Other parts of the app call this when something happens. */
@Service
public class NotificationService {

    public static final String CAFETERIA_REMINDER_KEY = "notifications.cafeteria.reminder";
    public static final String CAFETERIA_MENU_CHANGED_KEY = "notifications.cafeteria.menuChanged";
    private static final String CAFETERIA_LINK = "/cafeteria";
    private static final String DATE_SEPARATOR = ", ";

    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               EnrollmentRepository enrollmentRepository,
                               AnnouncementRepository announcementRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
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

    /**
     * The weekly "do not forget to reserve" reminder for every student.
     * It stays until the student reads it. If last week's reminder is still unread, no second one is made:
     * the old one just moves to the top. Returns how many students were reminded.
     */
    @Transactional
    public int notifyCafeteriaReminder() {
        Map<Long, Notification> unread = unreadByUser(CAFETERIA_REMINDER_KEY);
        List<Notification> notifications = new ArrayList<>();
        for (User student : students()) {
            Notification notification = unread.get(student.getId());
            if (notification != null) {
                notification.setCreatedAt(Instant.now());
            } else {
                notification = create(student, NotificationType.GENERAL, "Cafeteria reminder", CAFETERIA_LINK);
                notification.setMessageKey(CAFETERIA_REMINDER_KEY);
            }
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    /**
     * Tells every student that the menu of some days changed. If a student has not read an earlier
     * "menu changed" notification, the new days are added to it instead of making another one.
     */
    @Transactional
    public void notifyCafeteriaMenuChanged(List<LocalDate> days) {
        if (days.isEmpty()) {
            return;
        }
        Map<Long, Notification> unread = unreadByUser(CAFETERIA_MENU_CHANGED_KEY);
        List<Notification> notifications = new ArrayList<>();
        for (User student : students()) {
            // The days are kept as "2026-11-01, 2026-11-02" (sorted, no repeats).
            TreeSet<String> allDays = new TreeSet<>();
            days.forEach(day -> allDays.add(day.toString()));

            Notification notification = unread.get(student.getId());
            if (notification != null) {
                if (notification.getMessageArg() != null) {
                    allDays.addAll(Arrays.asList(notification.getMessageArg().split(DATE_SEPARATOR)));
                }
                notification.setCreatedAt(Instant.now());
            } else {
                notification = create(student, NotificationType.GENERAL, "Cafeteria menu changed", CAFETERIA_LINK);
                notification.setMessageKey(CAFETERIA_MENU_CHANGED_KEY);
            }
            notification.setMessageArg(String.join(DATE_SEPARATOR, allDays));
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);
    }

    private List<User> students() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList();
    }

    /** Unread notifications with this message key, by user id. */
    private Map<Long, Notification> unreadByUser(String messageKey) {
        Map<Long, Notification> result = new HashMap<>();
        for (Notification notification : notificationRepository.findByMessageKeyAndSeenFalse(messageKey)) {
            result.putIfAbsent(notification.getUser().getId(), notification);
        }
        return result;
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
