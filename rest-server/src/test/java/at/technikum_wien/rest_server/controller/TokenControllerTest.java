package at.technikum_wien.rest_server.controller;

import at.technikum_wien.rest_server.config.ApiTokenAuthentication;
import at.technikum_wien.rest_server.model.ApiToken;
import at.technikum_wien.rest_server.model.AppUser;
import at.technikum_wien.rest_server.service.ApiTokenService;
import at.technikum_wien.rest_server.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenController.class)
@AutoConfigureMockMvc(addFilters = false) // controller logic only; the token filter has its own test
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiTokenService apiTokenService;

    @MockitoBean
    private AppUserService appUserService;

    private final AppUser alice = new AppUser();

    @BeforeEach
    void loginWithSession() {
        alice.setUsername("alice");
        Mockito.when(appUserService.getCurrentUser()).thenReturn(alice);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_returnsSecretOnce() throws Exception {
        ApiToken token = token(1L, "Laptop");
        Mockito.when(apiTokenService.create(alice, "Laptop", 30))
                .thenReturn(new ApiTokenService.CreatedToken(token, "dms_secret"));

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"validDays\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.secret").value("dms_secret"));
    }

    @Test
    void create_validatesRequest() throws Exception {
        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"validDays\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_neverContainsSecrets() throws Exception {
        Mockito.when(apiTokenService.list(alice)).thenReturn(List.of(token(1L, "Laptop"), token(2L, "PC")));

        mockMvc.perform(get("/api/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].secret").doesNotExist());
    }

    @Test
    void revoke_returns204() throws Exception {
        mockMvc.perform(delete("/api/tokens/1"))
                .andExpect(status().isNoContent());

        Mockito.verify(apiTokenService).revoke(alice, 1L);
    }

    @Test
    void tokenAuthenticatedRequests_cannotManageTokens() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new ApiTokenAuthentication(new User("alice", "x", List.of()), 9L));

        mockMvc.perform(post("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"another\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tokens")).andExpect(status().isForbidden());

        Mockito.verify(apiTokenService, Mockito.never()).create(any(), any(), any());
    }

    private static ApiToken token(Long id, String name) {
        ApiToken token = new ApiToken();
        token.setId(id);
        token.setName(name);
        token.setPrefix("dms_abcdef");
        token.setSecretHash("hash-must-not-leak");
        token.setCreatedAt(LocalDateTime.of(2026, 11, 18, 10, 0));
        token.setExpiresAt(LocalDateTime.of(2027, 2, 16, 10, 0));
        return token;
    }
}
