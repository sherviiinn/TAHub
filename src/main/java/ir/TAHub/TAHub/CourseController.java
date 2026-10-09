package ir.TAHub.TAHub;

import ir.TAHub.TAHub.dto.CourseForm;
import ir.TAHub.TAHub.model.*;
import ir.TAHub.TAHub.repository.*;
import ir.TAHub.TAHub.util.JoinCodes;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class CourseController {

    private static final int MAX_LENGTH = 100;

    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    private final EnrollmentRepository enrollmentRepository;

    public CourseController(CourseOfferingRepository offeringRepository,
                            CourseRepository courseRepository,
                            SemesterRepository semesterRepository,
                            UserRepository userRepository,
                            EnrollmentRepository enrollmentRepository) {
        this.offeringRepository = offeringRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /** Lists only the offerings of the active semester. Older ones are hidden, not deleted. */
    @GetMapping("/courses")
    public String list(Model model, Authentication authentication) {
        Role role = currentUser(authentication).getRole();
        Optional<Semester> active = semesterRepository.findByActiveTrue();

        List<CourseOffering> offerings = active
                .map(offeringRepository::findBySemesterOrderByCourseCodeAsc)
                .orElse(List.of());

        model.addAttribute("semester", active.orElse(null));
        model.addAttribute("offerings", offerings);
        model.addAttribute("canCreate", role == Role.ADMIN || role == Role.PROFESSOR);
        return "courses";
    }

    @GetMapping("/courses/new")
    public String showForm(Model model, Authentication authentication) {
        model.addAttribute("form", new CourseForm());
        addFormData(model, authentication);
        return "course-form";
    }
    /** Shows one offering. Old (archived) offerings can still be opened here, they are only hidden from the list. */
    @GetMapping("/courses/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User current = currentUser(authentication);

        boolean isStudent = current.getRole() == Role.STUDENT;
        boolean canManage = offering.isManagedBy(current);

        // The student's own enrollment (if any) decides which buttons and messages they see.
        EnrollmentStatus myStatus = null;
        if (isStudent) {
            myStatus = enrollmentRepository.findByStudentAndOffering(current, offering)
                    .map(Enrollment::getStatus)
                    .orElse(null);
        }

        model.addAttribute("offering", offering);
        model.addAttribute("isCurrentSemester", offering.getSemester().isActive());
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("isEnrolled", myStatus == EnrollmentStatus.ACTIVE);
        model.addAttribute("wasRemoved", myStatus == EnrollmentStatus.REMOVED);
        model.addAttribute("canManage", canManage);

        // Students only learn whether enrollment is open. The code itself goes only to people who manage the offering.
        model.addAttribute("enrollmentOpen", offering.getJoinCode() != null);
        model.addAttribute("joinCode", canManage ? offering.getJoinCode() : null);

        // Only the professor and admins see who is enrolled. Students do not see their classmates.
        if (canManage) {
            model.addAttribute("enrollments", enrollmentRepository
                    .findByOfferingAndStatusOrderByStudentFullNameAsc(offering, EnrollmentStatus.ACTIVE));
            model.addAttribute("removedEnrollments", enrollmentRepository
                    .findByOfferingAndStatusOrderByStudentFullNameAsc(offering, EnrollmentStatus.REMOVED));
        }
        return "course-detail";
    }

    @PostMapping("/courses")
    public String create(@ModelAttribute("form") CourseForm form,
                         Model model,
                         Authentication authentication) {

        // Codes are stored in upper case so "cs301" and "CS301" are the same course.
        form.setCode(trimOrEmpty(form.getCode()).toUpperCase(Locale.ROOT));
        form.setName(trimOrEmpty(form.getName()));

        User current = currentUser(authentication);
        String errorKey = findFirstError(form);

        Semester semester = null;
        User professor = null;
        Course course = null;

        if (errorKey == null) {
            semester = semesterRepository.findByActiveTrue().orElse(null);
            if (semester == null) {
                errorKey = "course.error.noActiveSemester";
            }
        }

        if (errorKey == null) {
            if (current.getRole() == Role.PROFESSOR) {
                // A professor can only offer courses for themselves.
                // We ignore professorId here so nobody can assign a course to someone else.
                professor = current;
            } else {
                professor = findProfessor(form.getProfessorId());
                if (professor == null) {
                    errorKey = "course.error.professorRequired";
                }
            }
        }

        if (errorKey == null) {
            // If the course code already exists we reuse that course (and its saved name).
            course = courseRepository.findByCode(form.getCode()).orElse(null);
            if (course != null
                    && offeringRepository.existsByCourseAndSemesterAndProfessor(course, semester, professor)) {
                errorKey = "course.error.alreadyOffered";
            }
        }

        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            addFormData(model, authentication);
            return "course-form";
        }

        if (course == null) {
            course = new Course();
            course.setCode(form.getCode());
            course.setName(form.getName());
            courseRepository.save(course);
        }

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setSemester(semester);
        offering.setProfessor(professor);
        offering.setJoinCode(JoinCodes.generate());
        offeringRepository.save(offering);

        return "redirect:/courses";
    }

    /** Returns the message key of the first problem, in the same order as the form fields. */
    private String findFirstError(CourseForm form) {
        if (form.getCode().isEmpty()) {
            return "course.error.codeRequired";
        }
        if (form.getName().isEmpty()) {
            return "course.error.nameRequired";
        }
        if (form.getCode().length() > MAX_LENGTH || form.getName().length() > MAX_LENGTH) {
            return "course.error.tooLong";
        }
        return null;
    }

    /** Returns the user only if the id exists AND that user is a professor. */
    private User findProfessor(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .filter(user -> user.getRole() == Role.PROFESSOR)
                .orElse(null);
    }

    private void addFormData(Model model, Authentication authentication) {
        model.addAttribute("isAdmin", currentUser(authentication).getRole() == Role.ADMIN);
        model.addAttribute("professors", userRepository.findByRoleOrderByFullNameAsc(Role.PROFESSOR));
        model.addAttribute("semester", semesterRepository.findByActiveTrue().orElse(null));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}