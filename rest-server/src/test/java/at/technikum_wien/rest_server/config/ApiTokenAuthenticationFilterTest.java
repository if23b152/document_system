package at.technikum_wien.rest_server.config;

import at.technikum_wien.rest_server.model.ApiToken;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.service.ApiTokenService;
import at.technikum_wien.rest_server.service.AppUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiTokenAuthenticationFilterTest {

    private final ApiTokenService tokens = Mockito.mock(ApiTokenService.class);
    private final AppUserService users = Mockito.mock(AppUserService.class);
    private final ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(tokens, users);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutHeader_passesThroughUnauthenticated() throws Exception {
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verify(tokens, never()).authenticate(any());
    }

    @Test
    void otherAuthorizationSchemes_areIgnored() throws Exception {
        FilterChain chain = Mockito.mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verify(tokens, never()).authenticate(any());
    }

    @Test
    void validToken_authenticatesThisRequestOnly() throws Exception {
        AppUser alice = new AppUser();
        alice.setUsername("alice");
        ApiToken token = new ApiToken();
        token.setId(3L);
        token.setUser(alice);
        when(tokens.authenticate("dms_secret")).thenReturn(Optional.of(token));
        when(users.loadUserByUsername("alice")).thenReturn(new User("alice", "x", List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer dms_secret");
        AtomicReference<Authentication> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertInstanceOf(ApiTokenAuthentication.class, seen.get());
        assertEquals("alice", seen.get().getName());
        assertEquals(3L, ((ApiTokenAuthentication) seen.get()).getTokenId());
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "context is cleared afterwards");
        assertNull(request.getSession(false), "no session is created for token requests");
    }

    @Test
    void invalidToken_isRejectedWith401() throws Exception {
        when(tokens.authenticate("dms_wrong")).thenReturn(Optional.empty());
        FilterChain chain = Mockito.mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer dms_wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("Bearer error=\"invalid_token\"", response.getHeader("WWW-Authenticate"));
        verify(chain, never()).doFilter(any(), any());
    }
}
