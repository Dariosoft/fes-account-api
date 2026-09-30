package com.friendlyeshop.account.controller;

import com.friendlyeshop.account.http.cookie.SessionCookieWriter;
import com.friendlyeshop.account.model.dto.SessionResponse;
import com.friendlyeshop.account.service.SessionService;
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
    private final SessionService sessionService;

    private final SessionCookieWriter sessionCookieWriter;

    public SessionController(SessionService sessionService, SessionCookieWriter sessionCookieWriter) {
        this.sessionService = sessionService;
        this.sessionCookieWriter = sessionCookieWriter;
    }

    @GetMapping("/session")
    public SessionResponse session(
            @CookieValue(value = SessionCookieWriter.COOKIE_NAME, required = false) UUID sessionId) {
        return sessionService.read(sessionId);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = SessionCookieWriter.COOKIE_NAME, required = false) UUID sessionId) {
        sessionService.logout(sessionId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookieWriter.clear().toString())
                .build();
    }
}
