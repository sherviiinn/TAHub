package ir.TAHub.TAHub.config;

import ir.TAHub.TAHub.model.Major;
import ir.TAHub.TAHub.model.Course;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.*;
import ir.TAHub.TAHub.util.JoinCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Fills a brand new database with sample users, courses and enrollments,
 * so developers (and the frontend) have something to look at.
 * It runs only when tahub.demo-data=true AND the database is empty.
 * DEV ONLY: all demo accounts share a known, weak password.
 */
@Component
@Order(1) // Runs before DataInitializer, so the demo semesters are created in the right order.
@ConditionalOnProperty(name = "tahub.demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "demo12345";
    private final MajorRepository majorRepository;

    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
                          SemesterRepository semesterRepository,
                          CourseRepository courseRepository,
                          CourseOfferingRepository offeringRepository,
                          EnrollmentRepository enrollmentRepository,
                          PasswordEncoder passwordEncoder,
                          MajorRepository majorRepository) {

        this.userRepository = userRepository;
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.offeringRepository = offeringRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.majorRepository = majorRepository;
    }

    @Override
    public void run(String... args) {
        // Only for a brand new database, so real data is never touched.
        if (userRepository.count() > 0 || semesterRepository.count() > 0) {
            return;
        }

        // The old semester is created first so it gets the smaller id.
        Semester previous = semester("4051", false);
        Semester current = semester("4052", true);

        Major computer = major("Computer Engineering");
        Major electrical = major("Electrical Engineering");
        Major mechanical = major("Mechanical Engineering");


        User prof1 = user("Dr. Ali Karimi", "9001", Role.PROFESSOR, null);
        User prof2 = user("Dr. Maryam Hosseini", "9002", Role.PROFESSOR, null);

        User sara = user("Sara Ahmadi", "40000001", Role.STUDENT, computer);
        User reza = user("Reza Mohammadi", "40000002", Role.STUDENT, computer);
        User nima = user("Nima Rahimi", "40000003", Role.STUDENT, electrical);
        User tina = user("Tina Karimi", "40000004", Role.STUDENT, computer);
        User omid = user("Omid Hashemi", "40000005", Role.STUDENT, mechanical);
        User lena = user("Lena Sadeghi", "40000006", Role.STUDENT, computer);

        Course cs101 = course("CS101", "Introduction to Programming");
        Course cs201 = course("CS201", "Data Structures");
        Course cs301 = course("CS301", "Operating Systems");

        // Previous semester (archived).
        CourseOffering oldCs101 = offering(cs101, previous, prof1);
        CourseOffering oldCs201 = offering(cs201, previous, prof1);
        enroll(sara, oldCs101, EnrollmentStatus.ACTIVE);
        enroll(reza, oldCs101, EnrollmentStatus.ACTIVE);
        enroll(nima, oldCs101, EnrollmentStatus.ACTIVE);
        enroll(tina, oldCs101, EnrollmentStatus.ACTIVE);
        enroll(sara, oldCs201, EnrollmentStatus.ACTIVE);
        enroll(reza, oldCs201, EnrollmentStatus.ACTIVE);

        // Current semester. CS101 is not offered yet, so "Offer again" is visible for professor 1.
        CourseOffering newCs201 = offering(cs201, current, prof1);
        CourseOffering newCs301a = offering(cs301, current, prof1);
        CourseOffering newCs301b = offering(cs301, current, prof2);
        enroll(sara, newCs201, EnrollmentStatus.ACTIVE);
        enroll(reza, newCs201, EnrollmentStatus.ACTIVE);
        enroll(nima, newCs201, EnrollmentStatus.ACTIVE);
        enroll(tina, newCs201, EnrollmentStatus.DROPPED);
        enroll(omid, newCs201, EnrollmentStatus.REMOVED);
        enroll(sara, newCs301a, EnrollmentStatus.ACTIVE);
        enroll(lena, newCs301a, EnrollmentStatus.ACTIVE);
        enroll(reza, newCs301b, EnrollmentStatus.ACTIVE);
        enroll(nima, newCs301b, EnrollmentStatus.ACTIVE);

        log.info("Demo data created. All demo accounts use the password '{}'. Professors: 9001, 9002. Students: 40000001 to 40000006.",
                DEMO_PASSWORD);
    }

    private Semester semester(String code, boolean active) {
        Semester semester = new Semester();
        semester.setCode(code);
        semester.setActive(active);
        return semesterRepository.save(semester);
    }

    private Major major(String name) {
        Major major = new Major();
        major.setName(name);
        return majorRepository.save(major);
    }

    private User user(String fullName, String studentNumber, Role role, Major major) {
        User user = new User();
        user.setFullName(fullName);
        user.setStudentNumber(studentNumber);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setMajor(major);
        return userRepository.save(user);
    }

    private Course course(String code, String name) {
        Course course = new Course();
        course.setCode(code);
        course.setName(name);
        return courseRepository.save(course);
    }

    private CourseOffering offering(Course course, Semester semester, User professor) {
        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setSemester(semester);
        offering.setProfessor(professor);
        offering.setJoinCode(JoinCodes.generate());
        return offeringRepository.save(offering);
    }

    private void enroll(User student, CourseOffering offering, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setOffering(offering);
        enrollment.setEnrolledAt(Instant.now());
        enrollment.setStatus(status);
        enrollmentRepository.save(enrollment);
    }
}