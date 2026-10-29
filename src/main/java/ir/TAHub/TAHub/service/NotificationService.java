package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Notification;
import ir.TAHub.TAHub.model.NotificationType;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.SiteNotice;
import ir.TAHub.TAHub.model.TaInvitation;
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
    public static final String NOTICE_NEW_KEY = "notifications.notice.new";
    public static final String NOTICE_EDITED_KEY = "notifications.notice.edited";
    public static final String TELEGRAM_KEY = "notifications.telegram.updated";
    public static final String TA_ADDED_KEY = "notifications.ta.added";
    public static final String TA_PERMISSIONS_KEY = "notifications.ta.permissions";
    public static final String TA_REQUEST_KEY = "notifications.ta.request";
    public static final String TA_INVITED_KEY = "notifications.ta.invited";
    public static final String TA_INVITATION_ACCEPTED_KEY = "notifications.ta.invitationAccepted";
    public static final String TA_INVITATION_DECLINED_KEY = "notifications.ta.invitationDeclined";
    private static final String MY_TA_REQUESTS_LINK = "/my-ta-requests";
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

    /** A new site notice: every user except its author is told ("Site notice: title"). */
    @Transactional
    public void notifySiteNotice(SiteNotice notice) {
        List<Notification> notifications = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user.getId().equals(notice.getAuthor().getId())) {
                continue;
            }
            Notification notification = create(user, NotificationType.GENERAL, "Site notice", notice.getLink());
            notification.setMessageKey(NOTICE_NEW_KEY);
            notification.setMessageArg(notice.getTitle());
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * A site notice was edited. The title in its old notifications is updated. Every user except the editor
     * gets ONE "updated" notification per notice (an unread one just moves to the top).
     */
    @Transactional
    public void notifySiteNoticeEdited(SiteNotice notice, User editor) {
        String link = notice.getLink();
        Map<Long, Notification> unreadEdited = new HashMap<>();
        List<Notification> notifications = new ArrayList<>();

        for (Notification old : notificationRepository.findByLink(link)) {
            old.setMessageArg(notice.getTitle());
            notifications.add(old);
            if (!old.isSeen() && NOTICE_EDITED_KEY.equals(old.getMessageKey())) {
                unreadEdited.put(old.getUser().getId(), old);
            }
        }

        for (User user : userRepository.findAll()) {
            if (user.getId().equals(editor.getId())) {
                continue;
            }
            Notification existing = unreadEdited.get(user.getId());
            if (existing != null) {
                existing.setCreatedAt(Instant.now());
            } else {
                Notification created = create(user, NotificationType.GENERAL, "Site notice updated", link);
                created.setMessageKey(NOTICE_EDITED_KEY);
                created.setMessageArg(notice.getTitle());
                notifications.add(created);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * The admin asked to tell everyone about a notice again. Every user except that admin ends up with
     * an UNREAD notification about it at the top of their list: an unread one moves up, otherwise a new
     * "Site notice: title" is made.
     */
    @Transactional
    public void renotifySiteNotice(SiteNotice notice, User admin) {
        String link = notice.getLink();
        Map<Long, Notification> unread = new HashMap<>();
        for (Notification old : notificationRepository.findByLink(link)) {
            if (!old.isSeen()) {
                unread.putIfAbsent(old.getUser().getId(), old);
            }
        }

        List<Notification> notifications = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user.getId().equals(admin.getId())) {
                continue;
            }
            Notification notification = unread.get(user.getId());
            if (notification != null) {
                notification.setCreatedAt(Instant.now());
            } else {
                notification = create(user, NotificationType.GENERAL, "Site notice", link);
                notification.setMessageKey(NOTICE_NEW_KEY);
                notification.setMessageArg(notice.getTitle());
            }
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);
    }

    /** A site notice was deleted: all its notifications (read or unread) are removed. */
    @Transactional
    public void onSiteNoticeDeleted(SiteNotice notice) {
        notificationRepository.deleteAll(notificationRepository.findByLink(notice.getLink()));
    }

    /**
     * The Telegram link of an offering was set or changed: every enrolled student is told
     * ("Telegram group link updated in CS201 - ..."). One unread notification per course: it just moves up.
     */
    @Transactional
    public void notifyTelegramLink(CourseOffering offering) {
        String title = offering.getCourse().getCode() + " - " + offering.getCourse().getName();
        String link = "/courses/" + offering.getId();

        Map<Long, Notification> unread = new HashMap<>();
        for (Notification old : notificationRepository.findByMessageKeyAndLinkAndSeenFalse(TELEGRAM_KEY, link)) {
            unread.putIfAbsent(old.getUser().getId(), old);
        }

        List<Notification> notifications = new ArrayList<>();
        for (Enrollment enrollment : enrollmentRepository
                .findByOfferingAndStatusOrderByStudentFullNameAsc(offering, EnrollmentStatus.ACTIVE)) {
            User student = enrollment.getStudent();
            Notification notification = unread.get(student.getId());
            if (notification != null) {
                notification.setCreatedAt(Instant.now());
            } else {
                notification = create(student, NotificationType.GENERAL, "Telegram link updated", link);
                notification.setMessageKey(TELEGRAM_KEY);
                notification.setMessageArg(title);
            }
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);
    }

    /** Someone was made a TA of an offering: "You are now a TA of CS201 - ...". */
    @Transactional
    public void notifyTaAdded(CourseOffering offering, User ta) {
        Notification notification = create(ta, NotificationType.GENERAL, "You are now a TA",
                "/courses/" + offering.getId());
        notification.setMessageKey(TA_ADDED_KEY);
        notification.setMessageArg(offering.getCourse().getCode() + " - " + offering.getCourse().getName());
        notificationRepository.save(notification);
    }

    /** A TA's permissions changed. One unread notification per course: it just moves up. */
    @Transactional
    public void notifyTaPermissionsChanged(CourseOffering offering, User ta) {
        String link = "/courses/" + offering.getId();
        Notification notification = notificationRepository
                .findByMessageKeyAndLinkAndSeenFalse(TA_PERMISSIONS_KEY, link).stream()
                .filter(n -> n.getUser().getId().equals(ta.getId()))
                .findFirst()
                .orElse(null);
        if (notification != null) {
            notification.setCreatedAt(Instant.now());
        } else {
            notification = create(ta, NotificationType.GENERAL, "Your TA permissions changed", link);
            notification.setMessageKey(TA_PERMISSIONS_KEY);
            notification.setMessageArg(offering.getCourse().getCode() + " - " + offering.getCourse().getName());
        }
        notificationRepository.save(notification);
    }

    /**
     * A student asked to be a TA: the professor of the offering is told, with a link to the TA requests page.
     * One unread notification per course: another request just moves it up.
     */
    @Transactional
    public void notifyTaRequest(CourseOffering offering) {
        User professor = offering.getProfessor();
        String course = offering.getCourse().getCode() + " - " + offering.getCourse().getName();
        String link = "/ta-requests";
        Notification notification = notificationRepository
                .findByMessageKeyAndLinkAndSeenFalse(TA_REQUEST_KEY, link).stream()
                .filter(n -> n.getUser().getId().equals(professor.getId()) && course.equals(n.getMessageArg()))
                .findFirst()
                .orElse(null);
        if (notification != null) {
            notification.setCreatedAt(Instant.now());
        } else {
            notification = create(professor, NotificationType.GENERAL, "New TA request", link);
            notification.setMessageKey(TA_REQUEST_KEY);
            notification.setMessageArg(course);
        }
        notificationRepository.save(notification);
    }

    /** A student was invited to be a TA: "You were invited to be a TA of CS201 - ...". */
    @Transactional
    public void notifyTaInvited(TaInvitation invitation) {
        Notification notification = create(invitation.getInvitee(), NotificationType.GENERAL,
                "TA invitation", MY_TA_REQUESTS_LINK);
        notification.setMessageKey(TA_INVITED_KEY);
        notification.setMessageArg(courseName(invitation.getOffering()));
        notificationRepository.save(notification);
    }

    /** An invitation was cancelled: its unread "you were invited" notification is removed. */
    @Transactional
    public void onTaInvitationCancelled(TaInvitation invitation) {
        String course = courseName(invitation.getOffering());
        notificationRepository.deleteAll(notificationRepository
                .findByMessageKeyAndLinkAndSeenFalse(TA_INVITED_KEY, MY_TA_REQUESTS_LINK).stream()
                .filter(n -> n.getUser().getId().equals(invitation.getInvitee().getId())
                        && course.equals(n.getMessageArg()))
                .toList());
    }

    /**
     * The student answered an invitation. The person who invited is told; if that was a TA,
     * the professor of the offering is told too. Text like "TA invitation accepted: Sara Ahmadi - CS201 - ...".
     */
    @Transactional
    public void notifyTaInvitationAnswered(TaInvitation invitation, boolean accepted) {
        String key = accepted ? TA_INVITATION_ACCEPTED_KEY : TA_INVITATION_DECLINED_KEY;
        String arg = invitation.getInvitee().getFullName() + " - " + courseName(invitation.getOffering());
        String link = "/courses/" + invitation.getOffering().getId();

        List<User> told = new ArrayList<>();
        told.add(invitation.getInvitedBy());
        User professor = invitation.getOffering().getProfessor();
        if (!professor.getId().equals(invitation.getInvitedBy().getId())) {
            told.add(professor);
        }
        List<Notification> notifications = new ArrayList<>();
        for (User user : told) {
            Notification notification = create(user, NotificationType.GENERAL, "TA invitation answered", link);
            notification.setMessageKey(key);
            notification.setMessageArg(arg);
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);
    }

    private String courseName(CourseOffering offering) {
        return offering.getCourse().getCode() + " - " + offering.getCourse().getName();
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
