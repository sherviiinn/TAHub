package ir.TAHub.TAHub.config;

import ir.TAHub.TAHub.model.Role;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at startup. Creates a default admin if there is none,
 * so the system can never end up with nobody who can manage users.
 * DEV ONLY: the password below must come from an environment variable in production.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
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
}