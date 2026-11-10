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
 * One extension of an assignment's deadline, for every student. Each has its own grade factor
 * (for example 80 = answers sent in this extra time get 80% of their grade; applied in phase 5).
 */
@Entity
@Table(name = "assignment_extensions")
public class AssignmentExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /** The deadline before this extension. */
    @Column(nullable = false)
    private Instant previousDeadline;

    @Column(nullable = false)
    private Instant newDeadline;

    /** 1 to 100. */
    @Column(nullable = false)
    private int factorPercent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    public AssignmentExtension() {
    }

    public Long getId() { return id; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    public Instant getPreviousDeadline() { return previousDeadline; }
    public void setPreviousDeadline(Instant previousDeadline) { this.previousDeadline = previousDeadline; }

    public Instant getNewDeadline() { return newDeadline; }
    public void setNewDeadline(Instant newDeadline) { this.newDeadline = newDeadline; }

    public int getFactorPercent() { return factorPercent; }
    public void setFactorPercent(int factorPercent) { this.factorPercent = factorPercent; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
