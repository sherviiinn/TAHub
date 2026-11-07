package ir.TAHub.TAHub.config;

import ir.TAHub.TAHub.repository.UserRepository;
import ir.TAHub.TAHub.security.DisabledAccountFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Anyone may see the login and registration pages.
                        .requestMatchers("/login", "/register", "/register/professor").permitAll()
                        // Only admins may manage users and use the admin pages.
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Only admins and professors may create courses.
                        .requestMatchers("/courses/new").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.POST, "/courses").hasAnyRole("ADMIN", "PROFESSOR")
                        // Everything else requires a logged-in user.
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        // A disabled account gets its own message instead of "wrong password".
                        .failureHandler((request, response, exception) -> response.sendRedirect(
                                request.getContextPath()
                                        + (exception instanceof DisabledException ? "/login?disabled" : "/login?error")))
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout"))
                // Signs out users whose account was disabled while they were signed in.
                .addFilterAfter(new DisabledAccountFilter(userRepository), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
