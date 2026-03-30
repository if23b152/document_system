package at.technikum_wien.rest_server.service;

import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.model.RegisterRequest;
import at.technikum_wien.rest_server.model.UserRole;
import at.technikum_wien.rest_server.repository.AppUserRepository;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(CONFLICT, "Username already exists.");
        }
        if (appUserRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(CONFLICT, "Email already exists.");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setTemporary(false);
        user.setCreatedAt(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    public AppUser createTemporaryUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String username = "guest_" + suffix;
        String rawPassword = username;

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

            appUserRepository.findByUsername("admin")
                    .filter(user -> user.getRole() != UserRole.ADMIN)
                    .ifPresent(user -> {
                        user.setRole(UserRole.ADMIN);
                        appUserRepository.save(user);
                    });
        };
    }
}
