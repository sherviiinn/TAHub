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
 * A head TA asks the professor to raise the TA team limit of an offering to a new number.
 * Approving sets the limit to that number. Rows are kept (history).
 */
@Entity
@Table(name = "ta_limit_requests")
public class TaLimitRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    /** The head TA who asked. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(nullable = false)
    private int requestedLimit;

    /** Why (optional). */
    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaLimitRequestStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    public TaLimitRequest() {
    }

    public Long getId() { return id; }

    public CourseOffering getOffering() { return offering; }
    public void setOffering(CourseOffering offering) { this.offering = offering; }

    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }

    public int getRequestedLimit() { return requestedLimit; }
    public void setRequestedLimit(int requestedLimit) { this.requestedLimit = requestedLimit; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public TaLimitRequestStatus getStatus() { return status; }
    public void setStatus(TaLimitRequestStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
