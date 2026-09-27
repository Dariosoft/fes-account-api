package com.friendlyeshop.account.session.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.config.AuthConfig;
import com.friendlyeshop.account.config.CorsConfig;
import com.friendlyeshop.account.session.application.GetSession;
import com.friendlyeshop.account.session.application.Logout;
import com.friendlyeshop.account.session.application.SessionView;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SessionController.class)
@Import({SessionCookieWriter.class, AuthConfig.class, CorsConfig.class})
class SessionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSession getSession;

    @MockitoBean
    private Logout logout;

    @Test
    void sessionReturnsUnauthenticatedWithoutCookie() throws Exception {
        when(getSession.execute(null)).thenReturn(SessionView.unauthenticated());

        mockMvc.perform(get("/accounts/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void sessionReturnsAccountWhenAuthenticated() throws Exception {
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(getSession.execute(sessionId))
                .thenReturn(SessionView.authenticated(accountId, "ada@example.com", "Ada"));

        mockMvc.perform(get("/accounts/session").cookie(
                        new jakarta.servlet.http.Cookie("fes_session", sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.name").value("Ada"));
    }

    @Test
    void logoutClearsCookie() throws Exception {
        UUID sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(logout.execute(any())).thenReturn(Logout.LogoutResult.cookieCleared());

        mockMvc.perform(post("/accounts/logout").cookie(
                        new jakarta.servlet.http.Cookie("fes_session", sessionId.toString())))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("fes_session=")))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(logout).execute(sessionId);
    }
}
