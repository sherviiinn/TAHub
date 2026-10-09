package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * "My courses": what the current student or professor has in each semester,
 * with a switcher for earlier semesters. Professors can also offer an earlier course again.
 */
@Controller
public class MyCoursesController {

    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;

    public MyCoursesController(UserRepository userRepository,
                               SemesterRepository semesterRepository,
                               CourseOfferingRepository offeringRepository,
                               EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.semesterRepository = semesterRepository;
        this.offeringRepository = offeringRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/my-courses")
    public String myCourses(@RequestParam(required = false) Long semester,
                            Model model,
                            Authentication authentication) {
        User current = currentUser(authentication);

        // Admins neither take nor teach courses.
        if (current.getRole() == Role.ADMIN) {
            return "redirect:/";
        }
        boolean isProfessor = current.getRole() == Role.PROFESSOR;
        Semester active = semesterRepository.findByActiveTrue().orElse(null);

        // Every offering this user is part of, in all semesters.
        List<CourseOffering> allOfferings = isProfessor
                ? offeringRepository.findByProfessorOrderBySemesterIdDescCourseCodeAsc(current)
                : enrollmentRepository.findByStudentAndStatus(current, EnrollmentStatus.ACTIVE).stream()
                .map(Enrollment::getOffering)
                .toList();

        // Semesters the user can switch between: the active one plus every semester they took part in.
        // The map is sorted by id, newest first.
        Map<Long, Semester> choices = new TreeMap<>(Comparator.reverseOrder());
        if (active != null) {
            choices.put(active.getId(), active);
        }
        for (CourseOffering offering : allOfferings) {
            choices.put(offering.getSemester().getId(), offering.getSemester());
        }

        Long selectedId = chooseSemesterId(semester, choices, active);

        List<CourseOffering> offerings = allOfferings.stream()
                .filter(o -> o.getSemester().getId().equals(selectedId))
                .sorted(Comparator.comparing(o -> o.getCourse().getCode()))
                .toList();

        model.addAttribute("isProfessor", isProfessor);
        model.addAttribute("semesters", choices.values());
        model.addAttribute("selectedSemesterId", selectedId);
        model.addAttribute("selectedIsActive", active != null && active.getId().equals(selectedId));
        model.addAttribute("offerings", offerings);
        model.addAttribute("activeSemester", active);
        model.addAttribute("reofferable", isProfessor ? findReofferable(allOfferings, active) : List.of());
        return "my-courses";
    }

    /** A professor offers a course they taught before in the active semester again. */
    @PostMapping("/courses/{id}/reoffer")
    public String reoffer(@PathVariable Long id, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering previous = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Only the professor who taught it may offer it again (we compare ids, not names).
        if (current.getRole() != Role.PROFESSOR || !previous.getProfessor().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Semester active = semesterRepository.findByActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));

        // If it is already offered this semester, do nothing (the database forbids duplicates too).
        if (!offeringRepository.existsByCourseAndSemesterAndProfessor(previous.getCourse(), active, current)) {
            CourseOffering next = new CourseOffering();
            next.setCourse(previous.getCourse());
            next.setSemester(active);
            next.setProfessor(current);
            offeringRepository.save(next);
        }
        return "redirect:/my-courses";
    }

    /** Uses the requested semester only if it is one of the user's choices, otherwise the active one. */
    private Long chooseSemesterId(Long requested, Map<Long, Semester> choices, Semester active) {
        if (requested != null && choices.containsKey(requested)) {
            return requested;
        }
        if (active != null) {
            return active.getId();
        }
        return choices.isEmpty() ? null : choices.keySet().iterator().next();
    }

    /**
     * Courses this professor taught in earlier semesters but has not offered in the active one.
     * Only the most recent earlier offering of each course is returned.
     */
    private List<CourseOffering> findReofferable(List<CourseOffering> allOfferings, Semester active) {
        List<CourseOffering> result = new ArrayList<>();
        if (active == null) {
            return result;
        }

        Set<Long> alreadyOffered = new HashSet<>();
        for (CourseOffering offering : allOfferings) {
            if (offering.getSemester().getId().equals(active.getId())) {
                alreadyOffered.add(offering.getCourse().getId());
            }
        }

        // allOfferings is sorted newest semester first, so the first one we meet per course is the latest.
        for (CourseOffering offering : allOfferings) {
            Long courseId = offering.getCourse().getId();
            if (!alreadyOffered.contains(courseId)) {
                result.add(offering);
                alreadyOffered.add(courseId);
            }
        }
        return result;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }
}