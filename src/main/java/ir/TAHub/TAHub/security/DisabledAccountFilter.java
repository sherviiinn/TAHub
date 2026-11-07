package ir.TAHub.TAHub.security;

import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Signs out a user who is already signed in when an admin disables their account (or releases its number).
 * Without this they could keep using their open session. It is created in SecurityConfig, NOT as a @Component,
 * so it runs once, inside the security chain, after the session's user is known.
 */
public class DisabledAccountFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public DisabledAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Optional<User> user = userRepository.findByStudentNumber(authentication.getName());
            if (user.isEmpty() || user.get().getDisabledAt() != null) {
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?disabled");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
