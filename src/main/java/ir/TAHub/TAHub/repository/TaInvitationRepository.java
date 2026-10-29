package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaInvitation;
import ir.TAHub.TAHub.model.TaInvitationStatus;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaInvitationRepository extends JpaRepository<TaInvitation, Long> {

    // The waiting invitations of one offering (for its course page).
    List<TaInvitation> findByOfferingAndStatusOrderByCreatedAtAsc(CourseOffering offering, TaInvitationStatus status);

    // The waiting invitations of one student (for "My TA requests").
    List<TaInvitation> findByInviteeAndStatusOrderByCreatedAtDesc(User invitee, TaInvitationStatus status);

    boolean existsByInviteeAndOfferingAndStatus(User invitee, CourseOffering offering, TaInvitationStatus status);

    // Used later by the head TA team limit: waiting invitations count as places taken.
    long countByOfferingAndStatus(CourseOffering offering, TaInvitationStatus status);
}
