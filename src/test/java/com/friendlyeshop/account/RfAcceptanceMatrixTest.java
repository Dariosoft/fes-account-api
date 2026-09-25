package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Acceptance coverage matrix for RF-1 … RF-13 (spec 001).
 */
@ExtendWith(MockitoExtension.class)
class RfAcceptanceMatrixTest {

    @Mock
    private GoogleIdentityService googleIdentityService;

    @Mock
    private AccountService accountService;

    @Mock
    private SessionService sessionService;

    private SessionCookieSupport sessionCookieSupport;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sessionCookieSupport = new SessionCookieSupport(new SessionCookieProperties(false, "Lax"));
        GoogleAuthProperties googleProps = new GoogleAuthProperties(
                "client-id", "secret", "http://localhost/callback", "https://accounts.google.com");
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StoreAuthController(
                                googleIdentityService,
                                accountService,
                                sessionService,
                                sessionCookieSupport,
                                googleProps),
                        new SessionController(sessionService, sessionCookieSupport),
                        new PanelAccountController(accountService, sessionService, sessionCookieSupport))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void rf1Rf2Rf9AccountOwnedIdentityWithoutRolesOrPassword() {
        assertThat(Account.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("passwordHash", "role");
    }

    @Test
    void rf3Rf4Rf7Rf9StoreGoogleLoginCreateReuseAndReject() throws Exception {
        Account account = new Account("shop@example.com", "Shop User");
        Session session = new Session(account);
        when(googleIdentityService.verifyIdToken("ok"))
                .thenReturn(new GoogleIdentity("shop@example.com", "Shop User"));
        when(accountService.findOrCreate("shop@example.com", "Shop User")).thenReturn(account);
        when(sessionService.create(account)).thenReturn(session);
        when(googleIdentityService.verifyIdToken("bad"))
                .thenThrow(new GoogleAuthenticationException("No se pudo validar la identidad con Google."));

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(SessionCookieSupport.COOKIE_NAME));

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"ok\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rf5Rf6Rf8Rf12Rf13MeLogoutAndSharedSession() throws Exception {
        Account account = new Account("me@example.com", "Me");
        Session session = new Session(account);
        UUID sessionId = session.getId();
        when(sessionService.resolve(sessionId)).thenReturn(Optional.of(session), Optional.empty());

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));

        mockMvc.perform(post("/accounts/session/logout")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isOk());
        verify(sessionService).invalidate(sessionId);

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, sessionId.toString())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/accounts/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rf10Rf11PanelLookupWithoutOauthLogin() throws Exception {
        Account account = new Account("panel@example.com", "Panel");
        when(accountService.findByEmail("panel@example.com")).thenReturn(Optional.of(account));
        when(accountService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/lookup").param("email", "panel@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("panel@example.com"));

        mockMvc.perform(get("/accounts/lookup").param("email", "missing@example.com"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/accounts/auth/panel/google")).andExpect(status().isNotFound());
        verify(sessionService, never()).create(any());
    }

    @Test
    void rf12EstablishSessionSharesSameCookieContract() throws Exception {
        Account account = new Account("shared@example.com", "Shared");
        Session session = new Session(account);
        when(accountService.findByEmail("shared@example.com")).thenReturn(Optional.of(account));
        when(sessionService.create(account)).thenReturn(session);
        when(sessionService.resolve(session.getId())).thenReturn(Optional.of(session));

        mockMvc.perform(post("/accounts/session/establish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"shared@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(SessionCookieSupport.COOKIE_NAME));

        mockMvc.perform(get("/accounts/me")
                        .cookie(new Cookie(SessionCookieSupport.COOKIE_NAME, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("shared@example.com"));
    }
}
