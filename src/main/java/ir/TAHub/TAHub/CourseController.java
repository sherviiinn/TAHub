package ir.TAHub.TAHub;

import ir.TAHub.TAHub.dto.CourseForm;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CourseController {

    private static final int MAX_LENGTH = 100;

    private final CourseOfferingRepository courseRepository;
    private final UserRepository userRepository;

    public CourseController(CourseOfferingRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses")
    public String list(Model model, Authentication authentication) {
        Role role = currentUser(authentication).getRole();
        model.addAttribute("courses", courseRepository.findAllByOrderBySemesterDescCodeAsc());
        model.addAttribute("canCreate", role == Role.ADMIN || role == Role.PROFESSOR);
        return "courses";
    }

    @GetMapping("/courses/new")
    public String showForm(Model model, Authentication authentication) {
        model.addAttribute("form", new CourseForm());
        addFormData(model, authentication);
        return "course-form";
    }

    @PostMapping("/courses")
    public String create(@ModelAttribute("form") CourseForm form,
                         Model model,
                         Authentication authentication) {

        form.setCode(trimOrEmpty(form.getCode()));
        form.setName(trimOrEmpty(form.getName()));
        form.setSemester(trimOrEmpty(form.getSemester()));

        User current = currentUser(authentication);
        String errorKey = findFirstError(form);
        User professor = null;

        if (errorKey == null) {
            if (current.getRole() == Role.PROFESSOR) {
                // A professor can only create courses for themselves.
                // We ignore professorId here so nobody can assign a course to someone else.
                professor = current;
            } else {
                professor = findProfessor(form.getProfessorId());
                if (professor == null) {
                    errorKey = "course.error.professorRequired";
                }
            }
        }

        if (errorKey != null) {
            model.addAttribute("errorKey", errorKey);
            addFormData(model, authentication);
            return "course-form";
        }

        CourseOffering course = new CourseOffering();
        course.setCode(form.getCode());
        course.setName(form.getName());
        course.setSemester(form.getSemester());
        course.setProfessor(professor);
        courseRepository.save(course);

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
        if (form.getSemester().isEmpty()) {
            return "course.error.semesterRequired";
        }
        if (form.getCode().length() > MAX_LENGTH
                || form.getName().length() > MAX_LENGTH
                || form.getSemester().length() > MAX_LENGTH) {
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
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByStudentNumber(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Logged-in user no longer exists"));
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}