package com.friendlyeshop.account;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HTTP contract for the shared session: opaque id in an HttpOnly cookie named {@value #COOKIE_NAME}.
 * Secure and SameSite are applied via {@link SessionCookieProperties}. Session ids are never logged.
 */
@Component
public class SessionCookieSupport {

    public static final String COOKIE_NAME = "FES_SESSION";

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCookieSupport.class);

    private final SessionCookieProperties properties;

    public SessionCookieSupport(SessionCookieProperties properties) {
        this.properties = properties;
    }

    public Optional<UUID> readSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .flatMap(this::parseUuid);
    }

    public void writeSessionId(HttpServletResponse response, UUID sessionId) {
        Cookie cookie = new Cookie(COOKIE_NAME, sessionId.toString());
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.secure());
        cookie.setPath("/");
        cookie.setAttribute("SameSite", properties.sameSite());
        response.addCookie(cookie);
        LOGGER.debug("Attached session cookie for authenticated request");
    }

    public void clearSessionId(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.secure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", properties.sameSite());
        response.addCookie(cookie);
        LOGGER.debug("Cleared session cookie");
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Ignoring malformed session cookie");
            return Optional.empty();
        }
    }
}
