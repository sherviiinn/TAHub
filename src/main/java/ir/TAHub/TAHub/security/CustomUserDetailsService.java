package ir.TAHub.TAHub.security;

import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.util.Digits;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Tells Spring Security how to load a user from our database during login.
 * The student number (or a professor's username) is used as the username.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Persian digits and spaces around are accepted; professor usernames are stored in lower case.
        String typed = Digits.toAscii(username == null ? "" : username).trim();
        Optional<User> found = userRepository.findByStudentNumber(typed);
        if (found.isEmpty()) {
            found = userRepository.findByStudentNumber(typed.toLowerCase(Locale.ROOT));
        }
        User user = found.orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Spring has its own "User" class, so we use its full name to avoid a clash with ours.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getStudentNumber())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(user.getDisabledAt() != null)
                .build();
    }
}
