package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A notice from the admins to everyone (like "the upload problem is fixed").
 * The newest notice with showInBanner=true is shown in a bar at the top of every page.
 * "deleted" only hides it (it cannot be brought back from inside the app).
 */
@Entity
@Table(name = "site_notices")
public class SiteNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    // Set when the title or text was changed after posting. Empty if it was never edited.
    private Instant editedAt;

    @Column(nullable = false)
    private boolean showInBanner;

    @Column(nullable = false)
    private boolean deleted;

    public SiteNotice() {
    }

    /** The place of this notice on the notices page (also used to find its notifications). */
    public String getLink() {
        return "/notices#notice-" + id;
    }

    public Long getId() { return id; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }

    public boolean isShowInBanner() { return showInBanner; }
    public void setShowInBanner(boolean showInBanner) { this.showInBanner = showInBanner; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
