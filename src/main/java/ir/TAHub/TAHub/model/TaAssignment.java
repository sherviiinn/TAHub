package ir.TAHub.TAHub.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A user who is a teaching assistant (TA) in one offering, with what they are allowed to do there.
 * One person can be TA in many offerings, but only once in the same offering.
 * Removing a TA only sets active = false, because the history (grading, evaluations) must stay.
 * Adding the same person again makes the old row active again, with new permissions.
 */
@Entity
@Table(name = "ta_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "offering_id"}))
public class TaAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant assignedAt;

    /** Stored in its own table ta_assignment_permissions (one row per permission). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ta_assignment_permissions", joinColumns = @JoinColumn(name = "ta_assignment_id"))
    @Column(name = "permission", length = 40)
    @Convert(converter = TaPermissionConverter.class)
    private Set<TaPermission> permissions = new HashSet<>();

    /**
     * Empty for TAs made before permissions existed: they get the defaults until someone saves their
     * permissions. This way old rows need no database change.
     */
    private Boolean permissionsSet;

    public TaAssignment() {
    }

    /** What this TA may do now (the defaults if nobody has chosen yet). */
    public Set<TaPermission> getEffectivePermissions() {
        if (!Boolean.TRUE.equals(permissionsSet)) {
            return TaPermission.defaults();
        }
        Set<TaPermission> result = EnumSet.noneOf(TaPermission.class);
        permissions.stream().filter(Objects::nonNull).forEach(result::add);
        return result;
    }

    /** Replaces the permissions (and marks them as chosen). */
    public void replacePermissions(Set<TaPermission> newPermissions) {
        permissions.clear();
        permissions.addAll(newPermissions);
        permissionsSet = true;
    }

    public boolean has(TaPermission permission) {
        return getEffectivePermissions().contains(permission);
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public CourseOffering getOffering() { return offering; }
    public void setOffering(CourseOffering offering) { this.offering = offering; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
}
