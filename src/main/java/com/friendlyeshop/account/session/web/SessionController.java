package com.friendlyeshop.account.session.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyeshop.account.session.application.GetSession;
import com.friendlyeshop.account.session.application.Logout;
import com.friendlyeshop.account.session.application.SessionView;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class SessionController {
    private final GetSession getSession;

    private final Logout logout;

    private final SessionCookieWriter sessionCookieWriter;

    public SessionController(GetSession getSession, Logout logout, SessionCookieWriter sessionCookieWriter) {
        this.getSession = getSession;
        this.logout = logout;
        this.sessionCookieWriter = sessionCookieWriter;
    }

    @GetMapping("/session")
    public SessionResponse session(
            @CookieValue(value = SessionCookieWriter.COOKIE_NAME, required = false) UUID sessionId) {
        return SessionResponse.from(getSession.execute(sessionId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = SessionCookieWriter.COOKIE_NAME, required = false) UUID sessionId) {
        logout.execute(sessionId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookieWriter.clear().toString())
                .build();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionResponse(boolean authenticated, UUID id, String email, String name) {
        static SessionResponse from(SessionView view) {
            if (!view.authenticated()) {
                return new SessionResponse(false, null, null, null);
            }
            return new SessionResponse(true, view.id(), view.email(), view.name());
        }
    }
}
