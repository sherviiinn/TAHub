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
 * A one-time code an admin gives a professor (in person or by message) so they can create their own
 * professor account at /register/professor. Valid for 14 days. Rows are kept (history).
 */
@Entity
@Table(name = "professor_invites")
public class ProfessorInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Who it is for (optional, only a reminder for the admin). */
    @Column(length = 100)
    private String forName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    /** The professor account made with it. */
    @ManyToOne
    @JoinColumn(name = "used_by_id")
    private User usedBy;

    @Column(nullable = false)
    private boolean cancelled;

    public ProfessorInvite() {
    }

    /** Can this code still be used right now? */
    public boolean isUsable() {
        return !cancelled && usedAt == null && Instant.now().isBefore(expiresAt);
    }

    /** ACTIVE, USED, CANCELLED or EXPIRED, for the admin's list. */
    public String getState() {
        if (usedAt != null) {
            return "USED";
        }
        if (cancelled) {
            return "CANCELLED";
        }
        return Instant.now().isBefore(expiresAt) ? "ACTIVE" : "EXPIRED";
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getForName() { return forName; }
    public void setForName(String forName) { this.forName = forName; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public User getUsedBy() { return usedBy; }
    public void setUsedBy(User usedBy) { this.usedBy = usedBy; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
