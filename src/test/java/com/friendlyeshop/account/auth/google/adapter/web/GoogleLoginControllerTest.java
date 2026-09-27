package com.friendlyeshop.account.auth.google.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.auth.google.application.CompleteGoogleLogin;
import com.friendlyeshop.account.auth.google.application.StartGoogleLogin;
import com.friendlyeshop.account.config.AuthConfig;
import com.friendlyeshop.account.config.CorsConfig;
import com.friendlyeshop.account.session.web.SessionCookieWriter;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GoogleLoginController.class)
@Import({AuthConfig.class, CorsConfig.class, SessionCookieWriter.class})
class GoogleLoginControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartGoogleLogin startGoogleLogin;

    @MockitoBean
    private CompleteGoogleLogin completeGoogleLogin;

    @Test
    void allowedReturnToRedirectsToGoogle() throws Exception {
        when(startGoogleLogin.execute("http://store.example.test"))
                .thenReturn(StartGoogleLogin.Result.redirect("https://accounts.google.com/o/oauth2/v2/auth"));

        mockMvc.perform(get("/accounts/login/google").param("return_to", "http://store.example.test"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://accounts.google.com/o/oauth2/v2/auth"));
    }

    @Test
    void missingOrInvalidReturnToReturnsBadRequestWithoutGoogleLocation() throws Exception {
        when(startGoogleLogin.execute(null)).thenReturn(StartGoogleLogin.Result.invalid());
        when(startGoogleLogin.execute("http://evil.example")).thenReturn(StartGoogleLogin.Result.invalid());

        mockMvc.perform(get("/accounts/login/google"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));

        mockMvc.perform(get("/accounts/login/google").param("return_to", "http://evil.example"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void callbackSuccessSetsSessionCookieAndRedirects() throws Exception {
        UUID sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(completeGoogleLogin.execute("code", "state"))
                .thenReturn(CompleteGoogleLogin.Result.succeeded("http://store.example.test", sessionId));

        mockMvc.perform(get("/accounts/login/google/callback")
                        .param("code", "code")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://store.example.test"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("fes_session=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString(sessionId.toString())));
    }

    @Test
    void callbackFailureRedirectsWithLoginErrorWithoutSessionCookie() throws Exception {
        when(completeGoogleLogin.execute("bad", "state"))
                .thenReturn(CompleteGoogleLogin.Result.failed("http://store.example.test?login_error=1"));

        mockMvc.perform(get("/accounts/login/google/callback")
                        .param("code", "bad")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://store.example.test?login_error=1"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
