package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaInvitation;
import ir.TAHub.TAHub.model.TaInvitationStatus;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.TaInvitationRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.NotificationStream;
import ir.TAHub.TAHub.service.OfferingAccess;
import ir.TAHub.TAHub.service.TaService;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Inviting and removing the TAs of an offering, and choosing what each TA may do.
 * Nobody becomes a TA without saying yes: "Add TA" sends an invitation (see TaInvitationController).
 * Who may invite and remove TAs: the professor (and admins), and TAs with "Manage TA team".
 *   A TA cannot remove themselves or another TA who also has "Manage TA team" (only the professor can).
 * Who may change a TA's permissions: the professor (and admins), and TAs with "Give permissions".
 *   Such a TA only changes the permissions they have themselves, never their own permissions, and never
 *   those of another TA who also has "Give permissions".
 * Head TAs: a head TA has "Manage TA team" and "Give permissions". There can be several. Only the professor
 *   makes or unmakes head TAs, and only the professor gives or takes those two permissions.
 * Team limit (offering.taTeamLimit): head TAs cannot invite when active TAs + waiting invitations reach it.
 *   The professor is never held to it.
 * There is no "restore" for removed TAs: they are simply added again (with new permissions).
 */
@Controller
public class TaController {

    /** One line of the permissions page. */
    public record PermissionRow(TaPermission permission, boolean checked, boolean editable) {
    }

    private static final int TEAM_LIMIT_MAX = 50;

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final TaAssignmentRepository taRepository;
    private final TaInvitationRepository invitationRepository;
    private final TaService taService;
    private final OfferingAccess access;
    private final NotificationService notificationService;
    private final NotificationStream stream;

    public TaController(UserRepository userRepository,
                        CourseOfferingRepository offeringRepository,
                        TaAssignmentRepository taRepository,
                        TaInvitationRepository invitationRepository,
                        TaService taService,
                        OfferingAccess access,
                        NotificationService notificationService,
                        NotificationStream stream) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.taRepository = taRepository;
        this.invitationRepository = invitationRepository;
        this.taService = taService;
        this.access = access;
        this.notificationService = notificationService;
        this.stream = stream;
    }

    @PostMapping("/courses/{id}/tas")
    public String add(@PathVariable Long id,
                      @RequestParam(defaultValue = "") String studentNumber,
                      Authentication authentication,
                      RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = findOpenOffering(id);
        boolean manager = access.isManager(current, offering);
        if (!manager && !access.can(current, offering, TaPermission.MANAGE_TEAM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Persian digits are accepted too.
        String number = Digits.toAscii(studentNumber).trim();
        User candidate = number.isEmpty() ? null : userRepository.findByStudentNumber(number).orElse(null);

        // The same rules as for becoming a TA are checked now (and again when the student accepts).
        String errorKey = candidate == null ? "ta.error.notFound" : taService.findProblem(candidate, offering);
        if (errorKey == null
                && invitationRepository.existsByInviteeAndOfferingAndStatus(candidate, offering, TaInvitationStatus.PENDING)) {
            errorKey = "ta.error.alreadyInvited";
        }
        if (errorKey == null && !manager && isTeamFull(offering)) {
            errorKey = "ta.error.teamFull";
        }
        if (errorKey != null) {
            redirect.addFlashAttribute("taErrorKey", errorKey);
            return "redirect:/courses/" + id;
        }

        TaInvitation invitation = new TaInvitation();
        invitation.setOffering(offering);
        invitation.setInvitee(candidate);
        invitation.setInvitedBy(current);
        invitation.setByManager(manager); // the professor's invitations start with the default permissions
        invitation.setStatus(TaInvitationStatus.PENDING);
        invitation.setCreatedAt(Instant.now());
        invitationRepository.save(invitation);

        notificationService.notifyTaInvited(invitation);
        redirect.addFlashAttribute("taInvitedName", candidate.getFullName());
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/tas/{userId}/remove")
    public String remove(@PathVariable Long id,
                         @PathVariable Long userId,
                         Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOpenOffering(id);
        TaAssignment target = findActiveTa(userId, offering);

        if (!access.canRemoveTa(current, offering, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        target.setActive(false); // history stays
        taRepository.save(target);
        return "redirect:/courses/" + id;
    }

    @GetMapping("/courses/{id}/tas/{userId}/permissions")
    public String permissionsForm(@PathVariable Long id,
                                  @PathVariable Long userId,
                                  Model model,
                                  Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOpenOffering(id);
        TaAssignment target = findActiveTa(userId, offering);
        if (!access.canEditTaPermissions(current, offering, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Set<TaPermission> editable = access.grantable(current, offering);
        Set<TaPermission> now = target.getEffectivePermissions();
        List<PermissionRow> rows = new ArrayList<>();
        for (TaPermission permission : TaPermission.values()) {
            rows.add(new PermissionRow(permission, now.contains(permission), editable.contains(permission)));
        }

        model.addAttribute("offering", offering);
        model.addAttribute("target", target);
        model.addAttribute("rows", rows);
        model.addAttribute("isManager", access.isManager(current, offering));
        return "ta-permissions";
    }

    @PostMapping("/courses/{id}/tas/{userId}/permissions")
    public String savePermissions(@PathVariable Long id,
                                  @PathVariable Long userId,
                                  @RequestParam(name = "permission", required = false) List<String> checked,
                                  Authentication authentication,
                                  RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = findOpenOffering(id);
        TaAssignment target = findActiveTa(userId, offering);
        if (!access.canEditTaPermissions(current, offering, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Set<TaPermission> editable = access.grantable(current, offering);
        Set<TaPermission> before = target.getEffectivePermissions();
        Set<TaPermission> after = EnumSet.noneOf(TaPermission.class);
        for (TaPermission permission : TaPermission.values()) {
            boolean wanted = checked != null && checked.contains(permission.name());
            // A box the editor may not change keeps its old value, whatever the form says.
            boolean keep = editable.contains(permission) ? wanted : before.contains(permission);
            if (keep) {
                after.add(permission);
            }
        }

        target.replacePermissions(after);
        taRepository.save(target);
        // Only the permission list changed, which Hibernate does not report as a change of the TA row,
        // so the open course pages are told here (the save above is already committed).
        stream.broadcast("offering:" + offering.getId());
        if (!after.equals(before)) {
            notificationService.notifyTaPermissionsChanged(offering, target.getUser());
            boolean wasHead = before.containsAll(TaPermission.headOnly());
            boolean isHead = after.containsAll(TaPermission.headOnly());
            if (wasHead != isHead) {
                notificationService.notifyHeadTa(offering, target.getUser(), isHead);
            }
        }
        redirect.addFlashAttribute("taPermissionsSavedFor", target.getUser().getFullName());
        return "redirect:/courses/" + id;
    }

    /** The professor makes a TA a head TA (gives "Manage TA team" and "Give permissions"). */
    @PostMapping("/courses/{id}/tas/{userId}/head")
    public String makeHead(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        return setHead(id, userId, true, authentication);
    }

    /** The professor takes the head TA role away (the person stays a TA with their other permissions). */
    @PostMapping("/courses/{id}/tas/{userId}/unhead")
    public String removeHead(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        return setHead(id, userId, false, authentication);
    }

    /** The professor sets the most TAs head TAs may have in the team. An empty box means no limit. */
    @PostMapping("/courses/{id}/ta-team-limit")
    public String setTeamLimit(@PathVariable Long id,
                               @RequestParam(defaultValue = "") String limit,
                               Authentication authentication,
                               RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = findOpenOffering(id);
        if (!access.isManager(current, offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String text = Digits.toAscii(limit).trim();
        Integer value = null;
        if (!text.isEmpty()) {
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                value = -1;
            }
            if (value < 1 || value > TEAM_LIMIT_MAX) {
                redirect.addFlashAttribute("teamLimitErrorKey", "ta.teamLimit.error.range");
                return "redirect:/courses/" + id;
            }
            // Not below the team the course already has (TAs and waiting invitations).
            if (value < teamSize(offering)) {
                redirect.addFlashAttribute("teamLimitErrorKey", "ta.teamLimit.error.belowCurrent");
                return "redirect:/courses/" + id;
            }
        }
        offering.setTaTeamLimit(value);
        offeringRepository.save(offering);
        redirect.addFlashAttribute("teamLimitSaved", true);
        return "redirect:/courses/" + id;
    }

    private String setHead(Long id, Long userId, boolean head, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = findOpenOffering(id);
        if (!access.isManager(current, offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        TaAssignment target = findActiveTa(userId, offering);
        if (target.isHead() == head) {
            return "redirect:/courses/" + id;
        }

        Set<TaPermission> permissions = target.getEffectivePermissions();
        if (head) {
            permissions.addAll(TaPermission.headOnly());
        } else {
            permissions.removeAll(TaPermission.headOnly());
        }
        target.replacePermissions(permissions);
        taRepository.save(target);
        stream.broadcast("offering:" + offering.getId()); // only the permission list changed (see savePermissions)
        notificationService.notifyHeadTa(offering, target.getUser(), head);
        return "redirect:/courses/" + id;
    }

    /** Active TAs plus waiting invitations: the places the team already uses. */
    private long teamSize(CourseOffering offering) {
        return taRepository.countByOfferingAndActiveTrue(offering)
                + invitationRepository.countByOfferingAndStatus(offering, TaInvitationStatus.PENDING);
    }

    private boolean isTeamFull(CourseOffering offering) {
        Integer limit = offering.getTaTeamLimit();
        return limit != null && teamSize(offering) >= limit;
    }

    // ---------- helpers ----------

    /** An uncancelled offering of the active semester (TAs are only changed there). */
    private CourseOffering findOpenOffering(Long id) {
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!access.isOpen(offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return offering;
    }

    private TaAssignment findActiveTa(Long userId, CourseOffering offering) {
        return userRepository.findById(userId)
                .flatMap(user -> taRepository.findByUserAndOffering(user, offering))
                .filter(TaAssignment::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
