package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Adding and removing the TAs of an offering, and choosing what each TA may do.
 * Who may add and remove TAs: the professor (and admins), and TAs with "Manage TA team".
 *   A TA cannot remove themselves or another TA who also has "Manage TA team" (only the professor can).
 * Who may change a TA's permissions: the professor (and admins), and TAs with "Give permissions".
 *   Such a TA only changes the permissions they have themselves, never their own permissions, and never
 *   those of another TA who also has "Give permissions".
 * There is no "restore" for removed TAs: they are simply added again (with new permissions).
 */
@Controller
public class TaController {

    /** One line of the permissions page. */
    public record PermissionRow(TaPermission permission, boolean checked, boolean editable) {
    }

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final TaAssignmentRepository taRepository;
    private final TaService taService;
    private final OfferingAccess access;
    private final NotificationService notificationService;

    public TaController(UserRepository userRepository,
                        CourseOfferingRepository offeringRepository,
                        TaAssignmentRepository taRepository,
                        TaService taService,
                        OfferingAccess access,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.taRepository = taRepository;
        this.taService = taService;
        this.access = access;
        this.notificationService = notificationService;
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

        // The professor's new TA starts with the defaults; a TA's new teammate starts with nothing,
        // and then gets permissions from someone who may give them.
        Set<TaPermission> start = manager ? TaPermission.defaults() : EnumSet.noneOf(TaPermission.class);
        String errorKey = candidate == null ? "ta.error.notFound" : taService.assign(candidate, offering, start);
        if (errorKey != null) {
            redirect.addFlashAttribute("taErrorKey", errorKey);
            return "redirect:/courses/" + id;
        }

        notificationService.notifyTaAdded(offering, candidate);
        // The professor goes straight to the permissions of the new TA, to change the defaults if needed.
        if (manager) {
            return "redirect:/courses/" + id + "/tas/" + candidate.getId() + "/permissions";
        }
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

        Set<TaPermission> editable = access.permissions(current, offering);
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

        Set<TaPermission> editable = access.permissions(current, offering);
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
        if (!after.equals(before)) {
            notificationService.notifyTaPermissionsChanged(offering, target.getUser());
        }
        redirect.addFlashAttribute("taPermissionsSavedFor", target.getUser().getFullName());
        return "redirect:/courses/" + id;
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
