package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * One place that answers "may this user do X in this offering?".
 * The professor of the offering (and admins) may do everything. An active TA may do what is in their
 * permissions. Nobody else may do anything here. Controllers still check the semester and cancellation.
 */
@Service
public class OfferingAccess {

    private final TaAssignmentRepository taRepository;

    public OfferingAccess(TaAssignmentRepository taRepository) {
        this.taRepository = taRepository;
    }

    public boolean isManager(User user, CourseOffering offering) {
        return offering.isManagedBy(user);
    }

    /** The user's active TA row in this offering, if they are a TA there. */
    public Optional<TaAssignment> activeTa(User user, CourseOffering offering) {
        return taRepository.findByUserAndOffering(user, offering).filter(TaAssignment::isActive);
    }

    /** Everything this user may do in the offering. */
    public Set<TaPermission> permissions(User user, CourseOffering offering) {
        if (isManager(user, offering)) {
            return EnumSet.allOf(TaPermission.class);
        }
        return activeTa(user, offering)
                .map(TaAssignment::getEffectivePermissions)
                .orElse(EnumSet.noneOf(TaPermission.class));
    }

    public boolean can(User user, CourseOffering offering, TaPermission permission) {
        return permissions(user, offering).contains(permission);
    }

    // ---------- rules about other TAs ----------

    /** The professor removes any TA. A TA with "Manage TA team" removes others who do not have it. */
    public boolean canRemoveTa(User current, CourseOffering offering, TaAssignment target) {
        if (isManager(current, offering)) {
            return true;
        }
        return can(current, offering, TaPermission.MANAGE_TEAM)
                && !target.getUser().getId().equals(current.getId())
                && !target.has(TaPermission.MANAGE_TEAM);
    }

    /**
     * The professor changes any TA's permissions. A TA with "Give permissions" changes others who do not
     * have it (and only the permissions they have themselves, which the page and the save both enforce).
     */
    public boolean canEditTaPermissions(User current, CourseOffering offering, TaAssignment target) {
        if (isManager(current, offering)) {
            return true;
        }
        return can(current, offering, TaPermission.GRANT_PERMISSIONS)
                && !target.getUser().getId().equals(current.getId())
                && !target.has(TaPermission.GRANT_PERMISSIONS);
    }

    /** Changes are only allowed in an uncancelled offering of the active semester. */
    public boolean isOpen(CourseOffering offering) {
        return offering.getSemester().isActive() && !offering.isCancelled();
    }
}
