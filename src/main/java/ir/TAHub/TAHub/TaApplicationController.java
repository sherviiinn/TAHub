package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.TaApplication;
import ir.TAHub.TAHub.model.TaApplicationStatus;
import ir.TAHub.TAHub.model.TaInvitationStatus;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.TaApplicationRepository;
import ir.TAHub.TAHub.repository.TaInvitationRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.TaService;
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
import java.util.List;

/** Students ask to become a TA, and the professor accepts, rejects or reconsiders the requests. */
@Controller
public class TaApplicationController {

    private static final int MAX_MESSAGE = 500;
    private static final String ALREADY_TA = "ta.error.already";

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final TaApplicationRepository applicationRepository;
    private final TaInvitationRepository invitationRepository;
    private final TaService taService;
    private final NotificationService notificationService;

    public TaApplicationController(UserRepository userRepository,
                                   CourseOfferingRepository offeringRepository,
                                   TaApplicationRepository applicationRepository,
                                   TaInvitationRepository invitationRepository,
                                   TaService taService,
                                   NotificationService notificationService) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.applicationRepository = applicationRepository;
        this.invitationRepository = invitationRepository;
        this.taService = taService;
        this.notificationService = notificationService;
    }

    // ---------- The professor opens or closes requests for an offering ----------

    @PostMapping("/courses/{id}/ta-applications/open")
    public String open(@PathVariable Long id, Authentication authentication) {
        return setOpen(id, true, authentication);
    }

    @PostMapping("/courses/{id}/ta-applications/close")
    public String close(@PathVariable Long id, Authentication authentication) {
        return setOpen(id, false, authentication);
    }

    private String setOpen(Long id, boolean open, Authentication authentication) {
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireManagedCurrent(offering, currentUser(authentication));
        offering.setTaApplicationsOpen(open);
        offeringRepository.save(offering);
        return "redirect:/courses/" + id;
    }

    // ---------- The student ----------

    @PostMapping("/courses/{id}/ta-applications")
    public String apply(@PathVariable Long id,
                        @RequestParam(defaultValue = "") String message,
                        Authentication authentication,
                        RedirectAttributes redirect) {
        User student = currentUser(authentication);
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (student.getRole() != Role.STUDENT || !offering.getSemester().isActive() || offering.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String text = message.trim();
        String errorKey = null;
        if (!offering.isTaApplicationsOpen()) {
            errorKey = "ta.apply.error.closed";
        } else if (text.length() > MAX_MESSAGE) {
            errorKey = "ta.apply.error.tooLong";
        } else {
            String problem = taService.findProblem(student, offering);
            if (problem != null) {
                // "ta.error.enrolled" becomes "ta.apply.error.enrolled": the same problem, worded for the student.
                errorKey = problem.replace("ta.error.", "ta.apply.error.");
            }
        }
        if (errorKey != null) {
            redirect.addFlashAttribute("taApplyErrorKey", errorKey);
            return "redirect:/courses/" + id;
        }

        TaApplication application = applicationRepository.findByStudentAndOffering(student, offering).orElse(null);
        if (application == null) {
            application = new TaApplication();
            application.setStudent(student);
            application.setOffering(offering);
        } else if (application.getStatus() != TaApplicationStatus.WITHDRAWN) {
            // Waiting, accepted or rejected: nothing to send again. A rejected request only comes back
            // when the professor reconsiders it.
            return "redirect:/courses/" + id;
        }
        application.setMessage(text.isEmpty() ? null : text);
        application.setStatus(TaApplicationStatus.PENDING);
        application.setCreatedAt(Instant.now());
        application.setDecidedAt(null);
        applicationRepository.save(application);

        // The professor of the course is told (with a link to the TA requests page).
        notificationService.notifyTaRequest(offering);
        return "redirect:/courses/" + id;
    }

    @PostMapping("/ta-applications/{appId}/withdraw")
    public String withdraw(@PathVariable Long appId, Authentication authentication) {
        User student = currentUser(authentication);
        TaApplication application = findApplication(appId);

        // A student can only withdraw their own request.
        if (!application.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (application.getStatus() == TaApplicationStatus.PENDING) {
            application.setStatus(TaApplicationStatus.WITHDRAWN);
            application.setDecidedAt(Instant.now());
            applicationRepository.save(application);
        }
        return "redirect:/my-ta-requests";
    }

    @GetMapping("/my-ta-requests")
    public String mine(Model model, Authentication authentication) {
        User student = currentUser(authentication);
        if (student.getRole() != Role.STUDENT) {
            return "redirect:/";
        }
        model.addAttribute("applications", applicationRepository.findByStudentOrderByIdDesc(student));
        // Invitations still waiting for an answer, only for offerings that are still open.
        model.addAttribute("invitations", invitationRepository
                .findByInviteeAndStatusOrderByCreatedAtDesc(student, TaInvitationStatus.PENDING).stream()
                .filter(i -> i.getOffering().getSemester().isActive() && !i.getOffering().isCancelled())
                .toList());
        return "my-ta-requests";
    }

    // ---------- The professor (or admin) decides ----------

    @GetMapping("/ta-requests")
    public String requests(Model model, Authentication authentication) {
        User current = currentUser(authentication);
        if (current.getRole() != Role.PROFESSOR && current.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("pending", listFor(current, TaApplicationStatus.PENDING));
        model.addAttribute("rejected", listFor(current, TaApplicationStatus.REJECTED));
        return "ta-requests";
    }

    @PostMapping("/ta-applications/{appId}/accept")
    public String accept(@PathVariable Long appId, Authentication authentication, RedirectAttributes redirect) {
        TaApplication application = findManagedApplication(appId, authentication);
        if (application.getStatus() == TaApplicationStatus.PENDING) {
            // The same rules as when the professor adds a TA by hand.
            String problem = taService.assign(application.getStudent(), application.getOffering());
            if (problem != null && !problem.equals(ALREADY_TA)) {
                redirect.addFlashAttribute("requestErrorKey", problem);
            } else {
                decide(application, TaApplicationStatus.ACCEPTED);
                if (problem == null) {
                    notificationService.notifyTaAdded(application.getOffering(), application.getStudent());
                }
                // Straight to the new TA's permissions, so the professor can change the defaults.
                return "redirect:/courses/" + application.getOffering().getId()
                        + "/tas/" + application.getStudent().getId() + "/permissions";
            }
        }
        return "redirect:/ta-requests";
    }

    @PostMapping("/ta-applications/{appId}/reject")
    public String reject(@PathVariable Long appId, Authentication authentication) {
        TaApplication application = findManagedApplication(appId, authentication);
        if (application.getStatus() == TaApplicationStatus.PENDING) {
            decide(application, TaApplicationStatus.REJECTED);
        }
        return "redirect:/ta-requests";
    }

    /** Undo a rejection: the request goes back to "waiting" so the professor can decide again. */
    @PostMapping("/ta-applications/{appId}/reconsider")
    public String reconsider(@PathVariable Long appId, Authentication authentication, RedirectAttributes redirect) {
        TaApplication application = findManagedApplication(appId, authentication);
        if (application.getStatus() == TaApplicationStatus.REJECTED) {
            // The student may have become a student of this course in the meantime.
            String problem = taService.findProblem(application.getStudent(), application.getOffering());
            if (problem != null) {
                redirect.addFlashAttribute("requestErrorKey", problem);
            } else {
                application.setStatus(TaApplicationStatus.PENDING);
                application.setDecidedAt(null);
                applicationRepository.save(application);
            }
        }
        return "redirect:/ta-requests";
    }

    // ---------- helpers ----------

    private void decide(TaApplication application, TaApplicationStatus status) {
        application.setStatus(status);
        application.setDecidedAt(Instant.now());
        applicationRepository.save(application);
    }

    /** Requests of the active semester for the offerings this user manages (an admin sees all). */
    private List<TaApplication> listFor(User current, TaApplicationStatus status) {
        List<TaApplication> all = current.getRole() == Role.ADMIN
                ? applicationRepository.findByStatusOrderByIdAsc(status)
                : applicationRepository.findByOfferingProfessorAndStatusOrderByIdAsc(current, status);
        return all.stream()
                .filter(a -> a.getOffering().getSemester().isActive() && !a.getOffering().isCancelled())
                .toList();
    }

    private TaApplication findManagedApplication(Long appId, Authentication authentication) {
        TaApplication application = findApplication(appId);
        requireManagedCurrent(application.getOffering(), currentUser(authentication));
        return application;
    }

    private TaApplication findApplication(Long appId) {
        return applicationRepository.findById(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** The user must manage the offering, and it must be an uncancelled offering of the active semester. */
    private void requireManagedCurrent(CourseOffering offering, User current) {
        if (!offering.isManagedBy(current) || !offering.getSemester().isActive() || offering.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
