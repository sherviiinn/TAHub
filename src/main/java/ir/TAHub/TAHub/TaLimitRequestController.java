package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaLimitRequest;
import ir.TAHub.TAHub.model.TaLimitRequestStatus;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.TaLimitRequestRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.NotificationService;
import ir.TAHub.TAHub.service.OfferingAccess;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

/**
 * A head TA asks the professor to raise the TA team limit; the professor approves or rejects.
 * Only when the offering has a limit, only one waiting request per offering, and only in an uncancelled
 * offering of the active semester. Unanswered requests expire after 7 days (TaHousekeeping).
 */
@Controller
public class TaLimitRequestController {

    private static final int LIMIT_MAX = 50;
    private static final int MESSAGE_MAX = 300;

    private final CourseOfferingRepository offeringRepository;
    private final TaLimitRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final OfferingAccess access;
    private final NotificationService notificationService;

    public TaLimitRequestController(CourseOfferingRepository offeringRepository,
                                    TaLimitRequestRepository requestRepository,
                                    UserRepository userRepository,
                                    OfferingAccess access,
                                    NotificationService notificationService) {
        this.offeringRepository = offeringRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.notificationService = notificationService;
    }

    /** A head TA asks for a higher limit. */
    @PostMapping("/courses/{id}/ta-limit-requests")
    public String request(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String requestedLimit,
                          @RequestParam(defaultValue = "") String message,
                          Authentication authentication,
                          RedirectAttributes redirect) {
        User current = currentUser(authentication);
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean head = access.activeTa(current, offering).map(TaAssignment::isHead).orElse(false);
        if (!head || !access.isOpen(offering) || offering.getTaTeamLimit() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // First problem only, in the order of the form.
        String errorKey = null;
        Integer wanted = null;
        try {
            wanted = Integer.parseInt(Digits.toAscii(requestedLimit).trim());
        } catch (NumberFormatException e) {
            errorKey = "ta.limitRequest.error.number";
        }
        String text = message.trim();
        if (errorKey == null && (wanted <= offering.getTaTeamLimit() || wanted > LIMIT_MAX)) {
            errorKey = "ta.limitRequest.error.range";
        } else if (errorKey == null && text.length() > MESSAGE_MAX) {
            errorKey = "ta.limitRequest.error.tooLong";
        } else if (errorKey == null
                && requestRepository.findFirstByOfferingAndStatus(offering, TaLimitRequestStatus.PENDING).isPresent()) {
            errorKey = "ta.limitRequest.error.alreadyWaiting";
        }
        if (errorKey != null) {
            redirect.addFlashAttribute("limitRequestErrorKey", errorKey);
            return "redirect:/courses/" + id;
        }

        TaLimitRequest request = new TaLimitRequest();
        request.setOffering(offering);
        request.setRequestedBy(current);
        request.setRequestedLimit(wanted);
        request.setMessage(text.isEmpty() ? null : text);
        request.setStatus(TaLimitRequestStatus.PENDING);
        request.setCreatedAt(Instant.now());
        requestRepository.save(request);

        notificationService.notifyLimitRequest(request);
        redirect.addFlashAttribute("limitRequestSent", true);
        return "redirect:/courses/" + id;
    }

    /** The professor says yes: the limit becomes the requested number. */
    @PostMapping("/ta-limit-requests/{requestId}/approve")
    public String approve(@PathVariable Long requestId, Authentication authentication) {
        TaLimitRequest request = findPendingForManager(requestId, authentication);
        CourseOffering offering = request.getOffering();
        Integer limit = offering.getTaTeamLimit();
        if (limit != null && limit < request.getRequestedLimit()) {
            offering.setTaTeamLimit(request.getRequestedLimit());
            offeringRepository.save(offering);
        }
        decide(request, TaLimitRequestStatus.APPROVED);
        notificationService.notifyLimitRequestAnswered(request, NotificationService.TA_LIMIT_APPROVED_KEY);
        return "redirect:/courses/" + offering.getId();
    }

    @PostMapping("/ta-limit-requests/{requestId}/reject")
    public String reject(@PathVariable Long requestId, Authentication authentication) {
        TaLimitRequest request = findPendingForManager(requestId, authentication);
        decide(request, TaLimitRequestStatus.REJECTED);
        notificationService.notifyLimitRequestAnswered(request, NotificationService.TA_LIMIT_REJECTED_KEY);
        return "redirect:/courses/" + request.getOffering().getId();
    }

    // ---------- helpers ----------

    private TaLimitRequest findPendingForManager(Long requestId, Authentication authentication) {
        User current = currentUser(authentication);
        TaLimitRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CourseOffering offering = request.getOffering();
        if (!access.isManager(current, offering) || !access.isOpen(offering)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (request.getStatus() != TaLimitRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return request;
    }

    private void decide(TaLimitRequest request, TaLimitRequestStatus status) {
        request.setStatus(status);
        request.setDecidedAt(Instant.now());
        requestRepository.save(request);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}
