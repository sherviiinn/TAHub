package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One notification for one user. When something happens for many users
 * (like an announcement for a whole class) every user gets their own row,
 * so each person can read it (seen = true) independently.
 */
@Entity
@Table(name = "notifications")
// Every time a notification is created, changed or removed, its owner's open pages are told (live count).
@EntityListeners(NotificationLiveListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    /** A path inside this app, like "/courses/5/announcements". It is always created by the server. */
    @Column(nullable = false, length = 255)
    private String link;

    /** How many events this notification stands for (like 3 new announcements in the same course). */
    @Column(nullable = false)
    private int itemCount = 1;

    /**
     * When the first event counted by this notification happened. If an announcement is deleted later,
     * this tells us whether it was part of this notification. It is optional, so old rows (and the
     * database update) need no special care.
     */
    private Instant firstEventAt;

    /**
     * Only for "announcement was edited" notifications (type GENERAL): the id of the edited announcement.
     * It lets us remove the notification if that announcement is deleted later. Empty for all other kinds.
     */
    private Long announcementId;

    /**
     * For notifications whose text comes from messages.properties (like the cafeteria reminder):
     * the message key, and one optional value for {0} in it. Empty for the older kinds.
     */
    @Column(length = 100)
    private String messageKey;

    @Column(length = 255)
    private String messageArg;

    @Column(nullable = false)
    private boolean seen;

    @Column(nullable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }

    public String getMessageArg() { return messageArg; }
    public void setMessageArg(String messageArg) { this.messageArg = messageArg; }

    public Long getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(Long announcementId) { this.announcementId = announcementId; }

    public Instant getFirstEventAt() { return firstEventAt; }
    public void setFirstEventAt(Instant firstEventAt) { this.firstEventAt = firstEventAt; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
