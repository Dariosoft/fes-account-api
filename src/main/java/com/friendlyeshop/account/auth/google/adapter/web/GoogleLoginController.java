package com.friendlyeshop.account.auth.google.adapter.web;

import com.friendlyeshop.account.auth.google.application.CompleteGoogleLogin;
import com.friendlyeshop.account.auth.google.application.StartGoogleLogin;
import com.friendlyeshop.account.session.domain.Session;
import com.friendlyeshop.account.session.web.SessionCookieWriter;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/login/google")
public class GoogleLoginController {
    private final StartGoogleLogin startGoogleLogin;

    private final CompleteGoogleLogin completeGoogleLogin;

    private final SessionCookieWriter sessionCookieWriter;

    public GoogleLoginController(
            StartGoogleLogin startGoogleLogin,
            CompleteGoogleLogin completeGoogleLogin,
            SessionCookieWriter sessionCookieWriter) {
        this.startGoogleLogin = startGoogleLogin;
        this.completeGoogleLogin = completeGoogleLogin;
        this.sessionCookieWriter = sessionCookieWriter;
    }

    @GetMapping
    public ResponseEntity<Void> start(@RequestParam(value = "return_to", required = false) String returnTo) {
        StartGoogleLogin.Result result = startGoogleLogin.execute(returnTo);
        if (result.validationError()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(result.redirectUrl())).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state) {
        CompleteGoogleLogin.Result result = completeGoogleLogin.execute(code, state);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.redirectUrl()));
        if (result.success() && result.sessionId() != null) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    sessionCookieWriter.write(result.sessionId(), Duration.ofDays(Session.DEFAULT_TTL_DAYS))
                            .toString());
        }
        return response.build();
    }
}
