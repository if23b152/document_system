package at.technikum_wien.rest_server.config;

import at.technikum_wien.rest_server.model.ApiToken;
import at.technikum_wien.rest_server.service.ApiTokenService;
import at.technikum_wien.rest_server.service.AppUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates requests with "Authorization: Bearer dms_..." for this request only (no session).
 * Requests without such a header continue with the normal session login; an invalid or expired
 * token is answered with 401 right away instead of silently falling back to anonymous access.
 */
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final ApiTokenService apiTokenService;
    private final AppUserService appUserService;
    // Keeps the login on the request, so it is still there when a streamed response (PDF download)
    // is finished in a second, asynchronous pass
    private final SecurityContextRepository requestOnly = new RequestAttributeSecurityContextRepository();

    public ApiTokenAuthenticationFilter(ApiTokenService apiTokenService, AppUserService appUserService) {
        this.apiTokenService = apiTokenService;
        this.appUserService = appUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<ApiToken> token = apiTokenService.authenticate(header.substring(BEARER.length()).trim());
        if (token.isEmpty()) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UserDetails user = appUserService.loadUserByUsername(token.get().getUser().getUsername());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new ApiTokenAuthentication(user, token.get().getId()));
        SecurityContextHolder.setContext(context);
        requestOnly.saveContext(context, request, response);
        try {
            chain.doFilter(request, response);
        } finally {
            // Never leak the token login into other requests on this thread
            SecurityContextHolder.clearContext();
        }
    }
}
