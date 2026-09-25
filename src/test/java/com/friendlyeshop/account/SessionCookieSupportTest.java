package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionCookieSupportTest {

    private SessionCookieSupport support;

    @BeforeEach
    void setUp() {
        support = new SessionCookieSupport(new SessionCookieProperties(false, "Lax"));
    }

    @Test
    void readsAndWritesSessionCookieWithoutExposingIdInAssertionsAsSecretLeak() {
        UUID sessionId = UUID.randomUUID();
        HttpServletResponse response = mock(HttpServletResponse.class);
        support.writeSessionId(response, sessionId);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie written = cookieCaptor.getValue();
        assertThat(written.getName()).isEqualTo(SessionCookieSupport.COOKIE_NAME);
        assertThat(written.isHttpOnly()).isTrue();
        assertThat(written.getSecure()).isFalse();
        assertThat(written.getValue()).isEqualTo(sessionId.toString());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[] {written});
        Optional<UUID> read = support.readSessionId(request);
        assertThat(read).contains(sessionId);
    }

    @Test
    void clearsSessionCookie() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        support.clearSessionId(response);
        verify(response).addCookie(argThat(cookie ->
                SessionCookieSupport.COOKIE_NAME.equals(cookie.getName())
                        && cookie.getMaxAge() == 0));
    }
}
