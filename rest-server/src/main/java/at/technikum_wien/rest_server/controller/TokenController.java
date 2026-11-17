package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.config.ApiTokenAuthentication;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.model.CreateTokenRequest;
import at.technikum_wien.rest_server.model.TokenResponse;
import at.technikum_wien.rest_server.service.ApiTokenService;
import at.technikum_wien.rest_server.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Personal access tokens of the logged-in user (e.g. for the Explorer integration).
 * Only usable with a browser session: a token can never create, list or revoke tokens.
 */
@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final ApiTokenService apiTokenService;
    private final AppUserService appUserService;

    public TokenController(ApiTokenService apiTokenService, AppUserService appUserService) {
        this.apiTokenService = apiTokenService;
        this.appUserService = appUserService;
    }

    @GetMapping
    public List<TokenResponse> list() {
        return apiTokenService.list(sessionUser()).stream().map(TokenResponse::of).toList();
    }

    @PostMapping
    public ResponseEntity<TokenResponse> create(@Valid @RequestBody CreateTokenRequest request) {
        var created = apiTokenService.create(sessionUser(), request.name(), request.validDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(TokenResponse.of(created.token(), created.secret()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        apiTokenService.revoke(sessionUser(), id);
        return ResponseEntity.noContent().build();
    }

    private AppUser sessionUser() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof ApiTokenAuthentication) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manage access tokens in the web UI.");
        }
        return appUserService.getCurrentUser();
    }
}
