package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StoreAuthControllerTest {

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
                "client-id",
                "secret",
                "http://localhost/callback",
                "https://accounts.google.com");
        StoreAuthController controller = new StoreAuthController(
                googleIdentityService,
                accountService,
                sessionService,
                sessionCookieSupport,
                googleProps);
        SessionController sessionController = new SessionController(sessionService, sessionCookieSupport);
        mockMvc = MockMvcBuilders.standaloneSetup(controller, sessionController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void googleOkWithNewEmailCreatesAccountAndSession() throws Exception {
        Account account = new Account("new@example.com", "New User");
        Session session = new Session(account);
        when(googleIdentityService.verifyIdToken("good-token"))
                .thenReturn(new GoogleIdentity("new@example.com", "New User"));
        when(accountService.findOrCreate("new@example.com", "New User")).thenReturn(account);
        when(sessionService.create(account)).thenReturn(session);

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"good-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.displayName").value("New User"))
                .andExpect(cookie().exists(SessionCookieSupport.COOKIE_NAME))
                .andExpect(cookie().httpOnly(SessionCookieSupport.COOKIE_NAME, true));

        verify(accountService).findOrCreate("new@example.com", "New User");
        verify(sessionService).create(account);
    }

    @Test
    void googleOkWithSameEmailReusesAccountWithoutDuplicating() throws Exception {
        Account account = new Account("same@example.com", "Same User");
        UUID accountId = account.getId();
        Session firstSession = new Session(account);
        Session secondSession = new Session(account);
        when(googleIdentityService.verifyIdToken("good-token"))
                .thenReturn(new GoogleIdentity("same@example.com", "Same User"));
        when(accountService.findOrCreate("same@example.com", "Same User")).thenReturn(account);
        when(sessionService.create(account)).thenReturn(firstSession, secondSession);

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"good-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("same@example.com"));

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"good-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("same@example.com"));

        assertThat(account.getId()).isEqualTo(accountId);
        verify(accountService, times(2)).findOrCreate(eq("same@example.com"), eq("Same User"));
        verify(sessionService, times(2)).create(account);
        verify(accountService, never()).findOrCreate(eq("other@example.com"), any());
    }

    @Test
    void googleFailDoesNotCreateSessionAndMeStaysUnauthenticated() throws Exception {
        when(googleIdentityService.verifyIdToken("bad-token"))
                .thenThrow(new GoogleAuthenticationException("No se pudo validar la identidad con Google."));

        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No se pudo validar la identidad con Google."))
                .andExpect(cookie().doesNotExist(SessionCookieSupport.COOKIE_NAME));

        verify(sessionService, never()).create(any());
        verify(accountService, never()).findOrCreate(any(), any());

        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No hay una persona autenticada."));
    }
}
