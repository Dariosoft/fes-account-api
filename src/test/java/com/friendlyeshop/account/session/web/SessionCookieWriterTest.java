package com.friendlyeshop.account.session.web;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.config.AuthProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class SessionCookieWriterTest {
    @Test
    void writesHttpOnlyLaxCookieWithoutSecureOnHttp() {
        SessionCookieWriter writer = new SessionCookieWriter(properties(false));

        ResponseCookie cookie = writer.write(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Duration.ofDays(30));

        assertThat(cookie.getName()).isEqualTo("fes_session");
        assertThat(cookie.getValue()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isEqualTo(".example.test");
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void writesSecureCookieWhenHttpsConfigured() {
        SessionCookieWriter writer = new SessionCookieWriter(properties(true));

        ResponseCookie cookie = writer.write(UUID.randomUUID(), Duration.ofDays(30));

        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    void clearUsesSameAttributesAndZeroMaxAge() {
        SessionCookieWriter writer = new SessionCookieWriter(properties(true));

        ResponseCookie cookie = writer.clear();

        assertThat(cookie.getName()).isEqualTo("fes_session");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isEqualTo(".example.test");
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    private AuthProperties properties(boolean secure) {
        AuthProperties properties = new AuthProperties();
        properties.setGoogleClientId("id");
        properties.setGoogleClientSecret("secret");
        properties.setSessionCookieDomain(".example.test");
        properties.setBrowserOrigins(java.util.List.of("http://store.example.test"));
        properties.setCookieSecure(secure);
        return properties;
    }
}
