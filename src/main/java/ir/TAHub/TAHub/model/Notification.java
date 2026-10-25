package ir.TAHub.TAHub.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One notification for one user. When something happens for many users
 * (like an announcement for a whole class) every user gets their own row,
 * so each person can read it (seen = true) independently.
 */
@Entity
@Table(name = "notifications")
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

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
