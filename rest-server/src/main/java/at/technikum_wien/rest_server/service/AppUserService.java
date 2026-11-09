package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.model.RegisterRequest;
import at.technikum_wien.rest_server.model.UserRole;
import at.technikum_wien.rest_server.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class AppUserService implements UserDetailsService {

    static final String ADMIN_USERNAME = "admin";
    static final String GUEST_PREFIX = "guest_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    // Optional: admin account is only created from an environment variable (APP_ADMIN_PASSWORD)
    @Value("${app.admin.password:}")
    private String adminPassword;

    public AppUserService(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Reserved names: "admin" is promoted to ADMIN on startup, "guest_" is used for demo accounts.
        // Allowing them in self-registration would be a privilege escalation / impersonation path.
        String lower = username.toLowerCase(Locale.ROOT);
        if (lower.equals(ADMIN_USERNAME) || lower.startsWith(GUEST_PREFIX)) {
            throw new ResponseStatusException(BAD_REQUEST, "Username is not allowed.");
        }
        if (appUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(CONFLICT, "Username already exists.");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(CONFLICT, "Email already exists.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setTemporary(false);
        user.setCreatedAt(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    public AppUser createTemporaryUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String username = GUEST_PREFIX + suffix;
        // Random password that is never revealed: previously password == username,
        // so anyone who saw a guest name could log into that account.
        String rawPassword = randomSecret();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@temp.local");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.USER);
        user.setTemporary(true);
        user.setCreatedAt(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void deleteUser(AppUser user) {
        appUserRepository.delete(user);
    }

    public AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return findByUsername(username);
    }

    public boolean currentUserIsAdmin() {
        return getCurrentUser().getRole() == UserRole.ADMIN;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = findByUsername(username);
        return new User(
                user.getUsername(),
                user.getPasswordHash(),
                authorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> authorities(AppUser user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Bean
    public ApplicationRunner adminBootstrapRunner() {
        return args -> {
            appUserRepository.findAll().stream()
                    .filter(user -> user.getRole() == null || user.getTemporary() == null)
                    .forEach(user -> {
                        if (user.getRole() == null) {
                            user.setRole(UserRole.USER);
                        }
                        if (user.getTemporary() == null) {
                            user.setTemporary(false);
                        }
                        appUserRepository.save(user);
                    });

            if (adminPassword != null && !adminPassword.isBlank()
                    && !appUserRepository.existsByUsername(ADMIN_USERNAME)) {
                AppUser admin = new AppUser();
                admin.setUsername(ADMIN_USERNAME);
                admin.setEmail(ADMIN_USERNAME + "@localhost");
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setRole(UserRole.ADMIN);
                admin.setTemporary(false);
                admin.setCreatedAt(LocalDateTime.now());
                appUserRepository.save(admin);
            }

            appUserRepository.findByUsername(ADMIN_USERNAME)
                    .filter(user -> user.getRole() != UserRole.ADMIN)
                    .ifPresent(user -> {
                        user.setRole(UserRole.ADMIN);
                        appUserRepository.save(user);
                    });
        };
    }
}
