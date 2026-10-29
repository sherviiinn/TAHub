package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An invitation to be a TA of one offering. Nobody becomes a TA without saying yes:
 * the professor (or a TA with "Manage TA team") invites, the student accepts or declines.
 * Rows are never deleted (history). At most one PENDING invitation per student and offering (checked in code).
 */
@Entity
@Table(name = "ta_invitations")
public class TaInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    /** The student who is invited. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    /** Who sent it: the professor, an admin, or a TA with "Manage TA team". */
    @ManyToOne(optional = false)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;

    /** True if the professor (or an admin) invited: the new TA then starts with the default permissions. */
    @Column(nullable = false)
    private boolean byManager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaInvitationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    public TaInvitation() {
    }

    public Long getId() { return id; }

    public CourseOffering getOffering() { return offering; }
    public void setOffering(CourseOffering offering) { this.offering = offering; }

    public User getInvitee() { return invitee; }
    public void setInvitee(User invitee) { this.invitee = invitee; }

    public User getInvitedBy() { return invitedBy; }
    public void setInvitedBy(User invitedBy) { this.invitedBy = invitedBy; }

    public boolean isByManager() { return byManager; }
    public void setByManager(boolean byManager) { this.byManager = byManager; }

    public TaInvitationStatus getStatus() { return status; }
    public void setStatus(TaInvitationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
