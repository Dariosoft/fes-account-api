package com.friendlyeshop.account.controller;

import com.friendlyeshop.account.constants.OAuthParameters;
import com.friendlyeshop.account.http.cookie.SessionCookieWriter;
import com.friendlyeshop.account.model.Session;
import com.friendlyeshop.account.model.dto.CompletedLogin;
import com.friendlyeshop.account.service.GoogleLoginService;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
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
    private final GoogleLoginService googleLoginService;

    private final SessionCookieWriter sessionCookieWriter;

    public GoogleLoginController(GoogleLoginService googleLoginService, SessionCookieWriter sessionCookieWriter) {
        this.googleLoginService = googleLoginService;
        this.sessionCookieWriter = sessionCookieWriter;
    }

    @GetMapping
    public ResponseEntity<Void> start(
            @RequestParam(value = OAuthParameters.RETURN_TO, required = false) String returnTo) {
        Optional<String> redirectUrl = googleLoginService.start(returnTo);
        if (redirectUrl.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl.get())).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(value = OAuthParameters.CODE, required = false) String code,
            @RequestParam(value = OAuthParameters.STATE, required = false) String state) {
        CompletedLogin result = googleLoginService.complete(code, state);
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
