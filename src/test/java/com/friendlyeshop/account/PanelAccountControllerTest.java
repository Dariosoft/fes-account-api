package com.friendlyeshop.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PanelAccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private SessionService sessionService;

    private SessionCookieSupport sessionCookieSupport;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sessionCookieSupport = new SessionCookieSupport(new SessionCookieProperties(false, "Lax"));
        PanelAccountController panelController = new PanelAccountController(
                accountService, sessionService, sessionCookieSupport);
        SessionController sessionController = new SessionController(sessionService, sessionCookieSupport);
        mockMvc = MockMvcBuilders.standaloneSetup(panelController, sessionController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void lookupExistingEmailReturnsNameAndEmail() throws Exception {
        Account account = new Account("panel@example.com", "Panel User");
        when(accountService.findByEmail("panel@example.com")).thenReturn(Optional.of(account));

        mockMvc.perform(get("/accounts/lookup").param("email", "panel@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("panel@example.com"))
                .andExpect(jsonPath("$.displayName").value("Panel User"));
    }

    @Test
    void lookupMissingEmailReturnsNotFound() throws Exception {
        when(accountService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/lookup").param("email", "missing@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró una cuenta para el correo indicado."));
    }

    @Test
    void noPanelGoogleOauthLoginRouteExists() throws Exception {
        mockMvc.perform(post("/accounts/auth/panel/google"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/accounts/auth/panel/google"))
                .andExpect(status().isNotFound());
    }

    @Test
    void establishSessionForExistingAccountIsUsableByMe() throws Exception {
        Account account = new Account("shared@example.com", "Shared User");
        Session session = new Session(account);
        when(accountService.findByEmail("shared@example.com")).thenReturn(Optional.of(account));
        when(sessionService.create(account)).thenReturn(session);
        when(sessionService.resolve(session.getId())).thenReturn(Optional.of(session));

        MvcResult established = mockMvc.perform(post("/accounts/session/establish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"shared@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(SessionCookieSupport.COOKIE_NAME))
                .andExpect(jsonPath("$.email").value("shared@example.com"))
                .andReturn();

        String sessionCookie = established.getResponse().getCookie(SessionCookieSupport.COOKIE_NAME).getValue();

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("shared@example.com"))
                .andExpect(jsonPath("$.displayName").value("Shared User"));
    }

    @Test
    void logoutClosesSharedSessionFromEstablish() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.resolve(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/accounts/session/logout")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isOk());

        verify(sessionService).invalidate(sessionId);
        verify(sessionService, never()).create(any());

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isUnauthorized());
    }
}
