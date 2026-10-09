package ir.TAHub.TAHub.config;

import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.SemesterRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at startup and makes sure the system has the minimum data it needs:
 * at least one admin and one active semester.
 * DEV ONLY: the admin password below must come from an environment variable in production.
 */
@Component @Order(2)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           SemesterRepository semesterRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.semesterRepository = semesterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createAdminIfMissing();
        createFirstSemesterIfMissing();
    }

    private void createAdminIfMissing() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = new User();
        admin.setFullName("Administrator");
        admin.setStudentNumber("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private void createFirstSemesterIfMissing() {
        if (semesterRepository.count() > 0) {
            return;
        }
        Semester first = new Semester();
        first.setCode("4052");
        first.setActive(true);
        semesterRepository.save(first);
    }
}