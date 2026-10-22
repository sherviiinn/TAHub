package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.RosterService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The professor pastes the class list of an offering. */
@Controller
public class RosterController {

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final RosterService rosterService;

    public RosterController(UserRepository userRepository,
                            CourseOfferingRepository offeringRepository,
                            RosterService rosterService) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.rosterService = rosterService;
    }
    @PostMapping("/courses/{id}/roster")
    public String importRoster(@PathVariable Long id,
                               @RequestParam(defaultValue = "") String numbers,
                               Authentication authentication,
                               RedirectAttributes redirect) {
        User current = userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!offering.isManagedBy(current) || !offering.getSemester().isActive() || offering.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        RosterService.ParsedList parsed = rosterService.parse(numbers);
        if (parsed.numbers().isEmpty()) {
            redirect.addFlashAttribute("rosterErrorKey", "roster.error.empty");
        } else if (parsed.numbers().size() > RosterService.MAX_ENTRIES) {
            redirect.addFlashAttribute("rosterErrorKey", "roster.error.tooMany");
        } else {
            redirect.addFlashAttribute("rosterResult",
                    rosterService.importNumbers(offering, parsed.numbers(), parsed.invalid()));
        }
        return "redirect:/courses/" + id;
    }
}