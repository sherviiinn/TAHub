package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * The professors section: a list of professors, and for each professor the courses they teach
 * this semester with their TAs. Every logged-in user can see it.
 * Later (phase 3) the office information of the professor will be shown on the detail page.
 */
@Controller
public class ProfessorController {

    /** One course of a professor this semester, with its TAs. */
    public record CourseRow(CourseOffering offering, List<TaAssignment> teachingAssistants) {
    }

    private final UserRepository userRepository;
    private final CourseOfferingRepository offeringRepository;
    private final TaAssignmentRepository taRepository;

    public ProfessorController(UserRepository userRepository,
                               CourseOfferingRepository offeringRepository,
                               TaAssignmentRepository taRepository) {
        this.userRepository = userRepository;
        this.offeringRepository = offeringRepository;
        this.taRepository = taRepository;
    }

    @GetMapping("/professors")
    public String list(Model model) {
        model.addAttribute("professors", userRepository.findByRoleOrderByFullNameAsc(Role.PROFESSOR));
        return "professors";
    }

    @GetMapping("/professors/{id}")
    public String detail(@PathVariable Long id, Model model) {
        // Only users who really are professors have a page (other ids give "not found").
        User professor = userRepository.findById(id)
                .filter(user -> user.getRole() == Role.PROFESSOR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Active semester only, and cancelled offerings are hidden, like in the course list.
        List<CourseRow> courses = offeringRepository.findByProfessorOrderBySemesterIdDescCourseCodeAsc(professor)
                .stream()
                .filter(o -> o.getSemester().isActive() && !o.isCancelled())
                .map(o -> new CourseRow(o, taRepository.findByOfferingAndActiveTrueOrderByUserFullNameAsc(o)))
                .toList();

        model.addAttribute("professor", professor);
        model.addAttribute("courses", courses);
        return "professor-detail";
    }
}