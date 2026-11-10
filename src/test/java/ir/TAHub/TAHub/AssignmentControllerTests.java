package ir.TAHub.TAHub;

import ir.TAHub.TAHub.model.Assignment;
import ir.TAHub.TAHub.model.Course;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentSource;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.AssignmentExtensionRepository;
import ir.TAHub.TAHub.repository.AssignmentRepository;
import ir.TAHub.TAHub.repository.CourseOfferingRepository;
import ir.TAHub.TAHub.repository.CourseRepository;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4 (assignments): who may see and who may change them, and the rules of publishing and extending.
 * Every test makes its own professor, students, course and offering (with unique names), so the tests
 * do not depend on each other or on their order.
 */
@SpringBootTest(properties = {"tahub.demo-data=false", "tahub.storage.dir=target/test-files"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AssignmentControllerTests {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final byte[] PDF = "%PDF-1.4\n% a tiny test file\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseOfferingRepository offeringRepository;
    @Autowired SemesterRepository semesterRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired TaAssignmentRepository taRepository;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired AssignmentExtensionRepository extensionRepository;

    private User professor;
    private User student;      // enrolled in the offering
    private User outsider;     // a student who is not in the offering
    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        Semester active = semesterRepository.findByActiveTrue().orElseThrow();
        professor = newUser(Role.PROFESSOR);
        student = newUser(Role.STUDENT);
        outsider = newUser(Role.STUDENT);
        offering = newOffering(active, professor);
        enroll(student, offering);
    }

    // ---------- who may create ----------

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mvc.perform(get("/courses/{id}/assignments", offering.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void professorCreatesAndPublishes_andStudentSeesIt() throws Exception {
        mvc.perform(createRequest(offering, "HW1", inDays(3), pdf(), "publish").with(as(professor)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/assignments/*"));

        List<Assignment> saved = assignmentRepository.findByOfferingAndDeletedFalseOrderByDeadlineDescIdDesc(offering);
        assertThat(saved).hasSize(1);
        Assignment hw = saved.get(0);
        assertThat(hw.isPublished()).isTrue();
        assertThat(hw.getQuestionFile()).isNotNull();
        assertThat(hw.getFirstDeadline()).isEqualTo(hw.getDeadline());

        mvc.perform(get("/assignments/{id}", hw.getId()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HW1")));
        mvc.perform(get("/assignments/{id}/question.pdf", hw.getId()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void enrolledStudentCannotCreate() throws Exception {
        mvc.perform(get("/courses/{id}/assignments/new", offering.getId()).with(as(student)))
                .andExpect(status().isForbidden());
        mvc.perform(createRequest(offering, "HW1", inDays(3), pdf(), "publish").with(as(student)))
                .andExpect(status().isForbidden());
        assertThat(countOf(offering)).isZero();
    }

    @Test
    void studentListDoesNotShowTheNewButton() throws Exception {
        mvc.perform(get("/courses/{id}/assignments", offering.getId()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/assignments/new"))));
    }

    @Test
    void taWithPermissionCanCreate() throws Exception {
        User ta = newUser(Role.STUDENT);
        makeTa(ta, offering, Set.of(TaPermission.MANAGE_ASSIGNMENTS));

        mvc.perform(createRequest(offering, "HW1", inDays(3), pdf(), "draft").with(as(ta)))
                .andExpect(status().is3xxRedirection());
        assertThat(countOf(offering)).isEqualTo(1);
    }

    @Test
    void taWithoutPermissionCannotCreate() throws Exception {
        User ta = newUser(Role.STUDENT);
        makeTa(ta, offering, Set.of(TaPermission.POST_ANNOUNCEMENTS));

        mvc.perform(get("/courses/{id}/assignments/new", offering.getId()).with(as(ta)))
                .andExpect(status().isForbidden());
        mvc.perform(createRequest(offering, "HW1", inDays(3), pdf(), "draft").with(as(ta)))
                .andExpect(status().isForbidden());
        assertThat(countOf(offering)).isZero();
    }

    @Test
    void taOfAnOldSemesterCannotCreate() throws Exception {
        Semester old = new Semester();
        old.setCode("old-" + COUNTER.incrementAndGet());
        old.setActive(false);
        semesterRepository.save(old);
        CourseOffering oldOffering = newOffering(old, professor);
        User ta = newUser(Role.STUDENT);
        makeTa(ta, oldOffering, Set.of(TaPermission.MANAGE_ASSIGNMENTS));

        mvc.perform(get("/courses/{id}/assignments/new", oldOffering.getId()).with(as(ta)))
                .andExpect(status().isForbidden());
        mvc.perform(createRequest(oldOffering, "HW1", inDays(3), pdf(), "draft").with(as(ta)))
                .andExpect(status().isForbidden());
        // Not the professor either: an archived semester is read-only.
        mvc.perform(createRequest(oldOffering, "HW1", inDays(3), pdf(), "draft").with(as(professor)))
                .andExpect(status().isForbidden());
        assertThat(countOf(oldOffering)).isZero();
    }

    // ---------- who may see ----------

    @Test
    void outsiderCannotSeeTheListOrThePdf() throws Exception {
        Assignment hw = saveAssignment(offering, true);

        mvc.perform(get("/courses/{id}/assignments", offering.getId()).with(as(outsider)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/assignments/{id}", hw.getId()).with(as(outsider)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/assignments/{id}/question.pdf", hw.getId()).with(as(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void draftsAreHiddenFromStudents() throws Exception {
        Assignment draft = saveAssignment(offering, false);

        mvc.perform(get("/courses/{id}/assignments", offering.getId()).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(draft.getTitle()))));
        mvc.perform(get("/assignments/{id}", draft.getId()).with(as(student)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/courses/{id}/assignments", offering.getId()).with(as(professor)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(draft.getTitle())));
    }

    // ---------- publishing rules ----------

    @Test
    void cannotPublishWithoutPdf() throws Exception {
        mvc.perform(createRequest(offering, "HW1", inDays(3), null, "publish").with(as(professor)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/courses/*/assignments/new"));
        assertThat(countOf(offering)).isZero();
    }

    @Test
    void cannotPublishWithDeadlineInThePast() throws Exception {
        mvc.perform(createRequest(offering, "HW1", inDays(-1), pdf(), "publish").with(as(professor)))
                .andExpect(redirectedUrlPattern("/courses/*/assignments/new"));
        assertThat(countOf(offering)).isZero();
    }

    @Test
    void aFileThatIsNotAPdfIsRefused() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("questionFile", "hw.pdf", "application/pdf",
                "this is not a pdf".getBytes(StandardCharsets.UTF_8));
        mvc.perform(createRequest(offering, "HW1", inDays(3), fake, "draft").with(as(professor)))
                .andExpect(redirectedUrlPattern("/courses/*/assignments/new"));
        assertThat(countOf(offering)).isZero();
    }

    // ---------- extending ----------

    @Test
    void factorMustBeFrom1To100() throws Exception {
        Assignment hw = saveAssignment(offering, true);
        Instant before = hw.getDeadline();

        for (String factor : List.of("0", "101", "abc", "")) {
            mvc.perform(post("/assignments/{id}/extend", hw.getId())
                            .param("newDeadline", inDays(5)).param("factor", factor)
                            .with(csrf()).with(as(professor)))
                    .andExpect(status().is3xxRedirection());
        }
        assertThat(assignmentRepository.findById(hw.getId()).orElseThrow().getDeadline()).isEqualTo(before);
        assertThat(extensionRepository.findByAssignmentOrderByCreatedAtAscIdAsc(hw)).isEmpty();
    }

    @Test
    void extendingMovesTheDeadlineAndShowsTheTag() throws Exception {
        Assignment hw = saveAssignment(offering, true);
        Instant first = hw.getDeadline();

        mvc.perform(post("/assignments/{id}/extend", hw.getId())
                        .param("newDeadline", inDays(5)).param("factor", "80")
                        .with(csrf()).with(as(professor)))
                .andExpect(status().is3xxRedirection());

        Assignment after = assignmentRepository.findById(hw.getId()).orElseThrow();
        assertThat(after.getDeadline()).isAfter(first);
        assertThat(after.getFirstDeadline()).isEqualTo(first);
        mvc.perform(get("/courses/{id}/assignments", offering.getId()).with(as(student)))
                .andExpect(content().string(containsString("Extended | 80%")));
    }

    @Test
    void studentCannotExtend() throws Exception {
        Assignment hw = saveAssignment(offering, true);
        mvc.perform(post("/assignments/{id}/extend", hw.getId())
                        .param("newDeadline", inDays(5)).param("factor", "100")
                        .with(csrf()).with(as(student)))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    // In Spring 7 the multipart builder is not a MockHttpServletRequestBuilder any more, so it is returned as it is.
    private MockMultipartHttpServletRequestBuilder createRequest(CourseOffering target, String title, String deadline,
                                                                 MockMultipartFile file, String action) {
        MockMultipartHttpServletRequestBuilder request = multipart("/courses/{id}/assignments", target.getId());
        if (file != null) {
            request.file(file);
        }
        return request.param("title", title)
                .param("chapter", "Ch1")
                .param("description", "")
                .param("deadline", deadline)
                .param("action", action)
                .with(csrf());
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("questionFile", "hw1.pdf", "application/pdf", PDF);
    }

    /** A datetime-local value N days from now, in Tehran time (what the browser would send). */
    private String inDays(int days) {
        return LocalDateTime.now(TEHRAN).plusDays(days).truncatedTo(ChronoUnit.MINUTES).format(INPUT);
    }

    /** Makes an assignment through the real form (as the professor) and returns it. */
    private Assignment saveAssignment(CourseOffering target, boolean published) throws Exception {
        String title = "Saved-HW-" + COUNTER.incrementAndGet();
        mvc.perform(createRequest(target, title, inDays(3), pdf(), published ? "publish" : "draft").with(as(professor)))
                .andExpect(status().is3xxRedirection());
        return assignmentRepository.findByOfferingAndDeletedFalseOrderByDeadlineDescIdDesc(target).stream()
                .filter(a -> a.getTitle().equals(title))
                .findFirst().orElseThrow();
    }

    private long countOf(CourseOffering target) {
        return assignmentRepository.countByOfferingAndDeletedFalse(target);
    }

    /** Signs the request in as this user (the controllers find the user by this name). */
    private org.springframework.test.web.servlet.request.RequestPostProcessor as(User who) {
        return user(who.getStudentNumber()).roles(who.getRole().name());
    }

    private User newUser(Role role) {
        int n = COUNTER.incrementAndGet();
        User user = new User();
        user.setFullName(role.name().toLowerCase() + " " + n);
        user.setStudentNumber("t" + n + "-" + System.nanoTime());
        user.setPasswordHash("not-used-in-tests");
        user.setRole(role);
        return userRepository.save(user);
    }

    private CourseOffering newOffering(Semester semester, User teacher) {
        Course course = new Course();
        course.setCode("TST" + COUNTER.incrementAndGet() + "-" + System.nanoTime());
        course.setName("Test course");
        courseRepository.save(course);

        CourseOffering created = new CourseOffering();
        created.setCourse(course);
        created.setSemester(semester);
        created.setProfessor(teacher);
        return offeringRepository.save(created);
    }

    private void enroll(User who, CourseOffering target) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(who);
        enrollment.setOffering(target);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setSource(EnrollmentSource.JOIN_CODE);
        enrollment.setEnrolledAt(Instant.now());
        enrollmentRepository.save(enrollment);
    }

    private void makeTa(User who, CourseOffering target, Set<TaPermission> permissions) {
        TaAssignment ta = new TaAssignment();
        ta.setUser(who);
        ta.setOffering(target);
        ta.setActive(true);
        ta.setAssignedAt(Instant.now());
        ta.replacePermissions(permissions);
        taRepository.save(ta);
    }
}
