package com.friendlyeshop.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class SessionController {

    private final SessionService sessionService;

    private final SessionCookieSupport sessionCookieSupport;

    public SessionController(SessionService sessionService, SessionCookieSupport sessionCookieSupport) {
        this.sessionService = sessionService;
        this.sessionCookieSupport = sessionCookieSupport;
    }

    @GetMapping("/me")
    public AuthenticatedAccountResponse me(HttpServletRequest request) {
        UUID sessionId = sessionCookieSupport.readSessionId(request)
                .orElseThrow(UnauthenticatedException::new);
        Session session = sessionService.resolve(sessionId)
                .orElseThrow(UnauthenticatedException::new);
        Account account = session.getAccount();
        return new AuthenticatedAccountResponse(account.getDisplayName(), account.getEmail());
    }

    @PostMapping("/session/logout")
    public MapMessage logout(HttpServletRequest request, HttpServletResponse response) {
        sessionCookieSupport.readSessionId(request).ifPresent(sessionService::invalidate);
        sessionCookieSupport.clearSessionId(response);
        return new MapMessage("No hay una persona autenticada.");
    }

    public record MapMessage(String message) {
    }
}
