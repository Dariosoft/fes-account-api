package com.friendlyeshop.account.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.config.AuthConfig;
import com.friendlyeshop.account.config.CorsConfig;
import com.friendlyeshop.account.http.cookie.SessionCookieWriter;
import com.friendlyeshop.account.model.dto.CompletedLogin;
import com.friendlyeshop.account.service.GoogleLoginService;
import java.util.Optional;
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
    private GoogleLoginService googleLoginService;

    @Test
    void allowedReturnToRedirectsToGoogle() throws Exception {
        when(googleLoginService.start("http://store.example.test"))
                .thenReturn(Optional.of("https://accounts.google.com/o/oauth2/v2/auth"));

        mockMvc.perform(get("/accounts/login/google").param("return_to", "http://store.example.test"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://accounts.google.com/o/oauth2/v2/auth"));
    }

    @Test
    void missingOrInvalidReturnToReturnsBadRequestWithoutGoogleLocation() throws Exception {
        when(googleLoginService.start(null)).thenReturn(Optional.empty());
        when(googleLoginService.start("http://evil.example")).thenReturn(Optional.empty());

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
        when(googleLoginService.complete("code", "state"))
                .thenReturn(CompletedLogin.opened("http://store.example.test", sessionId));

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
        when(googleLoginService.complete("bad", "state"))
                .thenReturn(CompletedLogin.failed("http://store.example.test?login_error=1"));

        mockMvc.perform(get("/accounts/login/google/callback")
                        .param("code", "bad")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://store.example.test?login_error=1"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
