package com.friendlyeshop.account;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-to-server panel contracts. No panel Google OAuth login is exposed here (RF-10).
 */
@RestController
@RequestMapping("/accounts")
public class PanelAccountController {

    private final AccountService accountService;

    private final SessionService sessionService;

    private final SessionCookieSupport sessionCookieSupport;

    public PanelAccountController(
            AccountService accountService,
            SessionService sessionService,
            SessionCookieSupport sessionCookieSupport) {
        this.accountService = accountService;
        this.sessionService = sessionService;
        this.sessionCookieSupport = sessionCookieSupport;
    }

    @GetMapping("/lookup")
    public AuthenticatedAccountResponse lookupByEmail(@RequestParam("email") String email) {
        Account account = accountService.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException(email));
        return new AuthenticatedAccountResponse(account.getDisplayName(), account.getEmail());
    }

    @PostMapping("/session/establish")
    @ResponseStatus(HttpStatus.OK)
    public AuthenticatedAccountResponse establishSession(
            @Valid @RequestBody EstablishSessionRequest request,
            HttpServletResponse response) {
        Account account = accountService.findByEmail(request.email())
                .orElseThrow(() -> new AccountNotFoundException(request.email()));
        Session session = sessionService.create(account);
        sessionCookieSupport.writeSessionId(response, session.getId());
        return new AuthenticatedAccountResponse(account.getDisplayName(), account.getEmail());
    }
}
