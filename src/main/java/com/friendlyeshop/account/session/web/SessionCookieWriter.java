package com.friendlyeshop.account.session.web;

import com.friendlyeshop.account.config.AuthProperties;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieWriter {
    public static final String COOKIE_NAME = "fes_session";

    private final AuthProperties authProperties;

    public SessionCookieWriter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public ResponseCookie write(UUID sessionId, Duration maxAge) {
        return baseCookie(sessionId.toString(), maxAge);
    }

    public ResponseCookie clear() {
        return baseCookie("", Duration.ZERO);
    }

    private ResponseCookie baseCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .domain(authProperties.getSessionCookieDomain())
                .secure(authProperties.isCookieSecure())
                .maxAge(maxAge)
                .build();
    }
}
