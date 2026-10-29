package ir.TAHub.TAHub;

import ir.TAHub.TAHub.dto.CourseForm;
import ir.TAHub.TAHub.model.Course;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.TaApplication;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.TaApplicationStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.AnnouncementRepository;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.CourseRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.RosterEntryRepository;
import ir.TAHub.TAHub.repository.TaApplicationRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.service.OfferingAccess;
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
import java.util.Set;

@Controller
public class CourseController {

    private static final int MAX_LENGTH = 100;

    /** One TA as the course page shows it, with the buttons the current user may use. */
    public record TaRow(TaAssignment assignment, boolean canRemove, boolean canEditPermissions) {
    }

    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RosterEntryRepository rosterRepository;
    private final TaAssignmentRepository taRepository;
    private final TaApplicationRepository applicationRepository;
    private final AnnouncementRepository announcementRepository;
    private final OfferingAccess access;

    public CourseController(CourseOfferingRepository offeringRepository,
                            CourseRepository courseRepository,
                            SemesterRepository semesterRepository,
                            UserRepository userRepository,
                            EnrollmentRepository enrollmentRepository,
                            RosterEntryRepository rosterRepository,
                            TaAssignmentRepository taRepository,
                            TaApplicationRepository applicationRepository,
                            AnnouncementRepository announcementRepository,
                            OfferingAccess access) {
        this.offeringRepository = offeringRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.rosterRepository = rosterRepository;
        this.taRepository = taRepository;
        this.applicationRepository = applicationRepository;
        this.announcementRepository = announcementRepository;
        this.access = access;
    }

    /** Lists only the offerings of the active semester. Older ones are hidden, not deleted. */
    @GetMapping("/courses")
    public String list(Model model, Authentication authentication) {
        Role role = currentUser(authentication).getRole();
        Optional<Semester> active = semesterRepository.findByActiveTrue();

        List<CourseOffering> offerings = active
                .map(offeringRepository::findBySemesterAndCancelledFalseOrderByCourseCodeAsc)
                .orElse(List.of());

        model.addAttribute("semester", active.orElse(null));
        model.addAttribute("offerings", offerings);
        model.addAttribute("canCreate", role == Role.ADMIN || role == Role.PROFESSOR);
        return "courses";
    }

    /** Shows one offering. Old (archived) offerings can still be opened here, they are only hidden from the list. */
    @GetMapping("/courses/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User current = currentUser(authentication);

        boolean isStudent = current.getRole() == Role.STUDENT;
        boolean canManage = offering.isManagedBy(current);

        // A cancelled offering exists only for the people who manage it.
        if (offering.isCancelled() && !canManage) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // The student's own enrollment (if any) decides which buttons and messages they see.
        EnrollmentStatus myStatus = null;
        if (isStudent) {
            myStatus = enrollmentRepository.findByStudentAndOffering(current, offering)
                    .map(Enrollment::getStatus)
                    .orElse(null);
        }

        model.addAttribute("offering", offering);
        model.addAttribute("isCurrentSemester", offering.getSemester().isActive());
        model.addAttribute("isCancelled", offering.isCancelled());
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("isEnrolled", myStatus == EnrollmentStatus.ACTIVE);
        model.addAttribute("wasRemoved", myStatus == EnrollmentStatus.REMOVED);
        model.addAttribute("canManage", canManage);

        // What this user may do here: everything for the professor, a TA's own permissions, else nothing.
        Set<TaPermission> permissions = access.permissions(current, offering);
        boolean canManageStudents = permissions.contains(TaPermission.MANAGE_STUDENTS);
        boolean canSeeStudents = canManageStudents || permissions.contains(TaPermission.VIEW_STUDENTS);
        boolean canManageTeam = permissions.contains(TaPermission.MANAGE_TEAM);
        model.addAttribute("canManageStudents", canManageStudents);
        model.addAttribute("canSeeStudents", canSeeStudents);
        model.addAttribute("canManageTeam", canManageTeam);
        model.addAttribute("canEditTelegram", permissions.contains(TaPermission.EDIT_TELEGRAM));

        // Everybody can see who the TAs are. A TA cannot enroll as a student in the same offering.
        boolean open = access.isOpen(offering);
        List<TaRow> taRows = taRepository.findByOfferingAndActiveTrueOrderByUserFullNameAsc(offering).stream()
                .map(ta -> new TaRow(ta,
                        open && access.canRemoveTa(current, offering, ta),
                        open && access.canEditTaPermissions(current, offering, ta)))
                .toList();
        model.addAttribute("teachingAssistants", taRows);
        boolean isTa = taRepository.existsByUserAndOfferingAndActiveTrue(current, offering);
        model.addAttribute("isTa", isTa);
        // The same course can have several groups (offerings) this semester. A student of one group
        // cannot be a TA of another, and a TA of one group cannot be a student of another.
        boolean taOfOtherGroup = !isTa && isStudent && taRepository
                .existsByUserAndOfferingCourseAndOfferingSemesterAndActiveTrue(current, offering.getCourse(), offering.getSemester());
        boolean studentOfOtherGroup = isStudent && myStatus != EnrollmentStatus.ACTIVE && enrollmentRepository
                .existsByStudentAndOfferingCourseAndOfferingSemesterAndStatus(
                        current, offering.getCourse(), offering.getSemester(), EnrollmentStatus.ACTIVE);
        model.addAttribute("isTaOfOtherGroup", taOfOtherGroup);
        model.addAttribute("isStudentOfOtherGroup", studentOfOtherGroup);
        if (isTa) {
            // A TA sees what they are allowed to do here.
            model.addAttribute("myTaPermissions", permissions);
        }

        // Announcements are for the people of this offering: its managers, its TAs and its enrolled students.
        boolean canSeeAnnouncements = canManage || isTa || myStatus == EnrollmentStatus.ACTIVE;
        model.addAttribute("canSeeAnnouncements", canSeeAnnouncements);
        if (canSeeAnnouncements) {
            model.addAttribute("announcementCount", announcementRepository.countByOfferingAndDeletedFalse(offering));
        }

        // TA applications: is the offering taking them, and what is this student's own request (if any).
        model.addAttribute("taApplicationsOpen", offering.isTaApplicationsOpen());
        TaApplication myApplication = isStudent
                ? applicationRepository.findByStudentAndOffering(current, offering).orElse(null)
                : null;
        model.addAttribute("myTaApplication", myApplication);
        if (canManage) {
            model.addAttribute("pendingTaRequests",
                    applicationRepository.countByOfferingAndStatus(offering, TaApplicationStatus.PENDING));
        }

        // Students only learn whether enrollment is open. The code itself goes only to people who manage the offering.
        model.addAttribute("enrollmentOpen", offering.getJoinCode() != null);
        model.addAttribute("joinCode", canManageStudents ? offering.getJoinCode() : null);

        // Only the professor, admins and TAs with "View student list" or "Manage students" see who is enrolled.
        // Students do not see their classmates.
        if (canSeeStudents) {
            model.addAttribute("enrollments", enrollmentRepository
                    .findByOfferingAndStatusOrderByStudentFullNameAsc(offering, EnrollmentStatus.ACTIVE));
            model.addAttribute("removedEnrollments", enrollmentRepository
                    .findByOfferingAndStatusOrderByStudentFullNameAsc(offering, EnrollmentStatus.REMOVED));
        }
        if (canManageStudents) {
            // Numbers on the class list that have no account yet.
            model.addAttribute("pendingRoster", rosterRepository.findByOfferingOrderByStudentNumberAsc(offering).stream()
                    .filter(entry -> !userRepository.existsByStudentNumber(entry.getStudentNumber()))
                    .toList());
        }
        return "course-detail";
    }

    @PostMapping("/courses/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication) {
        CourseOffering offering = findManagedCurrentOffering(id, authentication);

        // We do not leave enrolled students without a course. The professor must remove them first.
        if (enrollmentRepository.countByOfferingAndStatus(offering, EnrollmentStatus.ACTIVE) > 0) {
            return "redirect:/courses/" + id + "?cancelError";
        }

        offering.setCancelled(true);
        offering.setJoinCode(null); // enrollment is closed
        offeringRepository.save(offering);
        return "redirect:/courses/" + id;
    }

    @PostMapping("/courses/{id}/uncancel")
    public String uncancel(@PathVariable Long id, Authentication authentication) {
        CourseOffering offering = findManagedCurrentOffering(id, authentication);
        offering.setCancelled(false);
        offering.setJoinCode(JoinCodes.generate()); // a fresh code, since the old one was removed
        offeringRepository.save(offering);
        return "redirect:/courses/" + id;
    }

    /** Loads an offering of the ACTIVE semester and checks that the current user manages it. */
    private CourseOffering findManagedCurrentOffering(Long id, Authentication authentication) {
        User current = currentUser(authentication);
        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!offering.isManagedBy(current) || !offering.getSemester().isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return offering;
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
            if (course != null) {
                CourseOffering existing = offeringRepository
                        .findByCourseAndSemesterAndProfessor(course, semester, professor).orElse(null);
                if (existing != null) {
                    errorKey = existing.isCancelled() ? "course.error.cancelledExists" : "course.error.alreadyOffered";
                }
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
