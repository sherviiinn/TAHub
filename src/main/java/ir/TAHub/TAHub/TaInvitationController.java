package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaInvitation;
import ir.TAHub.TAHub.model.TaInvitationStatus;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.TaInvitationRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.OfferingAccess;
import ir.TAHub.TAHub.service.TaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Answering and cancelling TA invitations.
 * Accept / decline: only the invited student. Cancel: the person who invited, or the professor (and admins).
 * Only for uncancelled offerings of the active semester. The rules for becoming a TA are checked again
 * on accept, because things may have changed since the invitation (for example the student enrolled).
 */
@Controller
public class TaInvitationController {

    private final TaInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TaService taService;
    private final OfferingAccess access;
    private final NotificationService notificationService;

    public TaInvitationController(TaInvitationRepository invitationRepository,
                                  UserRepository userRepository,
                                  TaService taService,
                                  OfferingAccess access,
                                  NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.taService = taService;
        this.access = access;
        this.notificationService = notificationService;
    }

    @PostMapping("/ta-invitations/{id}/accept")
    public String accept(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        User current = currentUser(authentication);
        TaInvitation invitation = findPendingOf(id, current);
        CourseOffering offering = invitation.getOffering();

        // The professor's invitation starts with the default permissions, a TA's invitation with none.
        Set<TaPermission> start = invitation.isByManager() ? TaPermission.defaults() : EnumSet.noneOf(TaPermission.class);
        String problem = taService.assign(current, offering, start);
        if (problem != null && !problem.equals("ta.error.already")) {
            redirect.addFlashAttribute("invitationErrorKey", problem.replace("ta.error.", "ta.apply.error."));
            return "redirect:/my-ta-requests";
        }

        decide(invitation, TaInvitationStatus.ACCEPTED);
        notificationService.notifyTaInvitationAnswered(invitation, true);
        return "redirect:/courses/" + offering.getId();
    }

    @PostMapping("/ta-invitations/{id}/decline")
    public String decline(@PathVariable Long id, Authentication authentication) {
        User current = currentUser(authentication);
        TaInvitation invitation = findPendingOf(id, current);
        decide(invitation, TaInvitationStatus.DECLINED);
        notificationService.notifyTaInvitationAnswered(invitation, false);
        return "redirect:/my-ta-requests";
    }

    @PostMapping("/ta-invitations/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication) {
        User current = currentUser(authentication);
        TaInvitation invitation = findInvitation(id);
        CourseOffering offering = invitation.getOffering();

        boolean allowed = access.isManager(current, offering)
                || invitation.getInvitedBy().getId().equals(current.getId());
        if (!allowed || !access.isOpen(offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (invitation.getStatus() == TaInvitationStatus.PENDING) {
            decide(invitation, TaInvitationStatus.CANCELLED);
            notificationService.onTaInvitationCancelled(invitation);
        }
        return "redirect:/courses/" + offering.getId();
    }

    // ---------- helpers ----------

    /** A waiting invitation for the current user, in an offering that is still open. */
    private TaInvitation findPendingOf(Long id, User current) {
        TaInvitation invitation = findInvitation(id);
        if (!invitation.getInvitee().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (invitation.getStatus() != TaInvitationStatus.PENDING || !access.isOpen(invitation.getOffering())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return invitation;
    }

    private void decide(TaInvitation invitation, TaInvitationStatus status) {
        invitation.setStatus(status);
        invitation.setDecidedAt(Instant.now());
        invitationRepository.save(invitation);
    }

    private TaInvitation findInvitation(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
