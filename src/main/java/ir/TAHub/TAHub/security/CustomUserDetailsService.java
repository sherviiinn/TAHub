package ir.TAHub.TAHub.security;

import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to load a user from our database during login.
 * The student number is used as the username.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String studentNumber) throws UsernameNotFoundException {
        User user = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Spring has its own "User" class, so we use its full name to avoid a clash with ours.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getStudentNumber())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}