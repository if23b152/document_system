package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.model.AuthResponse;
import at.technikum_wien.rest_server.model.LoginRequest;
import at.technikum_wien.rest_server.model.RegisterRequest;
import at.technikum_wien.rest_server.service.AppUserService;
import at.technikum_wien.rest_server.service.DocumentService;
import at.technikum_wien.rest_server.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;
    private final DocumentService documentService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AppUserService appUserService,
                          DocumentService documentService,
                          AuthenticationManager authenticationManager,
                          LoginAttemptService loginAttemptService) {
        this.appUserService = appUserService;
        this.documentService = documentService;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AppUser createdUser = appUserService.register(request);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        storeAuthentication(authentication, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.fromUser(createdUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (loginAttemptService.isBlocked(request.username(), clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            loginAttemptService.loginSucceeded(request.username(), clientIp);
            storeAuthentication(authentication, httpRequest);
            AppUser user = appUserService.findByUsername(authentication.getName());
            return ResponseEntity.ok(AuthResponse.fromUser(user));
        } catch (AuthenticationException ex) {
            // Same response for unknown user, wrong password, locked account, ...
            loginAttemptService.loginFailed(request.username(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/try")
    public ResponseEntity<AuthResponse> tryNow(HttpServletRequest httpRequest) {
        AppUser temporaryUser = appUserService.createTemporaryUser();
        // Guest accounts have a random, unknown password -> authenticate directly server-side
        UserDetails details = appUserService.loadUserByUsername(temporaryUser.getUsername());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()
        );
        storeAuthentication(authentication, httpRequest);
        return ResponseEntity.ok(AuthResponse.fromUser(temporaryUser));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            AppUser user = appUserService.findByUsername(authentication.getName());
            if (Boolean.TRUE.equals(user.getTemporary())) {
                documentService.deleteDocumentsForOwner(user);
                appUserService.deleteUser(user);
            }
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AppUser user = appUserService.findByUsername(authentication.getName());
        return ResponseEntity.ok(AuthResponse.fromUser(user));
    }

    private void storeAuthentication(Authentication authentication, HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Prevent session fixation: issue a new session id after authentication
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
