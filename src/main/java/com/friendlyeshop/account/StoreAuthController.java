package com.friendlyeshop.account;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class StoreAuthController {

    private final GoogleIdentityService googleIdentityService;

    private final AccountService accountService;

    private final SessionService sessionService;

    private final SessionCookieSupport sessionCookieSupport;

    private final GoogleAuthProperties googleAuthProperties;

    public StoreAuthController(
            GoogleIdentityService googleIdentityService,
            AccountService accountService,
            SessionService sessionService,
            SessionCookieSupport sessionCookieSupport,
            GoogleAuthProperties googleAuthProperties) {
        this.googleIdentityService = googleIdentityService;
        this.accountService = accountService;
        this.sessionService = sessionService;
        this.sessionCookieSupport = sessionCookieSupport;
        this.googleAuthProperties = googleAuthProperties;
    }

    @GetMapping("/auth/google")
    public Map<String, String> startGoogleLogin() {
        String clientId = googleAuthProperties.clientId() == null ? "" : googleAuthProperties.clientId();
        String redirectUri = googleAuthProperties.redirectUri() == null ? "" : googleAuthProperties.redirectUri();
        String authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=id_token"
                + "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&nonce=store-login";
        return Map.of("authorizationUrl", authorizationUrl);
    }

    @PostMapping("/auth/google")
    @ResponseStatus(HttpStatus.OK)
    public AuthenticatedAccountResponse completeGoogleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        GoogleIdentity identity = googleIdentityService.verifyIdToken(request.idToken());
        Account account = accountService.findOrCreate(identity.email(), identity.displayName());
        Session session = sessionService.create(account);
        sessionCookieSupport.writeSessionId(response, session.getId());
        return new AuthenticatedAccountResponse(account.getDisplayName(), account.getEmail());
    }
}
