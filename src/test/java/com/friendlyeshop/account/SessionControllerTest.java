package com.friendlyeshop.account;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    private SessionCookieSupport sessionCookieSupport;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sessionCookieSupport = new SessionCookieSupport(new SessionCookieProperties(false, "Lax"));
        mockMvc = MockMvcBuilders.standaloneSetup(new SessionController(sessionService, sessionCookieSupport))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void meWithActiveSessionReturnsNameAndEmail() throws Exception {
        Account account = new Account("ada@example.com", "Ada Lovelace");
        Session session = new Session(account);
        when(sessionService.resolve(session.getId())).thenReturn(Optional.of(session));

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void meWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No hay una persona autenticada."));
    }

    @Test
    void meWithInvalidSessionReturnsUnauthorized() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.resolve(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No hay una persona autenticada."));
    }

    @Test
    void logoutInvalidatesSessionAndMeStaysUnauthenticated() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.resolve(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/accounts/session/logout")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(SessionCookieSupport.COOKIE_NAME, 0));

        verify(sessionService).invalidate(sessionId);

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/accounts/session/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No hay una persona autenticada."));
    }

    @Test
    void afterLogoutOldSessionIdDoesNotAuthenticate() throws Exception {
        UUID oldSessionId = UUID.randomUUID();
        when(sessionService.resolve(oldSessionId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/accounts/session/logout")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, oldSessionId.toString())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, oldSessionId.toString())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No hay una persona autenticada."));
    }
}
