package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.service.GoogleOAuthClient;
import com.friendlyeshop.account.model.dto.GoogleProfile;
import com.friendlyeshop.account.service.OAuthState;
import com.friendlyeshop.account.controller.SessionCookieWriter;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SharedGoogleSessionAcceptanceTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void resetGoogle() {
        when(googleOAuthClient.exchangeCode(any()))
                .thenReturn(Optional.of(new GoogleProfile("shared-sub", "shared@example.com", "Shared")));
    }

    @Test
    void uniqueAccountSharedSessionAndLogout() throws Exception {
        String state = OAuthState.encode("http://store.example.test");

        MvcResult login = mockMvc.perform(get("/accounts/login/google/callback")
                        .param("code", "ok")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://store.example.test"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("fes_session=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Domain=.example.test")))
                .andReturn();

        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly").contains("Path=/");
        String sessionId = extractSessionId(setCookie);
        Cookie sessionCookie = new Cookie(SessionCookieWriter.COOKIE_NAME, sessionId);

        mockMvc.perform(get("/accounts/login/google/callback")
                        .param("code", "ok")
                        .param("state", state))
                .andExpect(status().isFound());

        var first = accountRepository.findByGoogleSub("shared-sub").orElseThrow();
        var secondLoginAccount = accountRepository.findByGoogleSub("shared-sub").orElseThrow();
        assertThat(secondLoginAccount.getId()).isEqualTo(first.getId());

        mockMvc.perform(get("/accounts/session")
                        .cookie(sessionCookie)
                        .header(HttpHeaders.ORIGIN, "http://store.example.test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.email").value("shared@example.com"))
                .andExpect(jsonPath("$.name").value("Shared"));

        mockMvc.perform(get("/accounts/session")
                        .cookie(sessionCookie)
                        .header(HttpHeaders.ORIGIN, "http://panel.example.test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.email").value("shared@example.com"));

        mockMvc.perform(post("/accounts/logout").cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")));

        mockMvc.perform(get("/accounts/session").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));

        mockMvc.perform(get("/accounts/session").cookie(sessionCookie)
                        .header(HttpHeaders.ORIGIN, "http://panel.example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    private static String extractSessionId(String setCookie) {
        String prefix = SessionCookieWriter.COOKIE_NAME + "=";
        int start = setCookie.indexOf(prefix) + prefix.length();
        int end = setCookie.indexOf(';', start);
        return setCookie.substring(start, end);
    }
}
