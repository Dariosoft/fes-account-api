package com.friendlyeshop.account.auth.google.application;

import com.friendlyeshop.account.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StartGoogleLogin {
    private final AuthProperties authProperties;

    private final GoogleAuthClient googleAuthClient;

    public StartGoogleLogin(AuthProperties authProperties, GoogleAuthClient googleAuthClient) {
        this.authProperties = authProperties;
        this.googleAuthClient = googleAuthClient;
    }

    public Result execute(String returnTo) {
        if (returnTo == null || returnTo.isBlank() || !isAllowedOrigin(returnTo)) {
            return Result.invalid();
        }
        String state = encodeState(returnTo);
        return Result.redirect(googleAuthClient.authorizationUrl(state));
    }

    private boolean isAllowedOrigin(String returnTo) {
        return authProperties.getBrowserOrigins().stream().anyMatch(returnTo::equals);
    }

    public static String encodeState(String returnTo) {
        String nonce = UUID.randomUUID().toString();
        String payload = nonce + "|" + returnTo;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<String> decodeReturnTo(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            int separator = payload.indexOf('|');
            if (separator < 0 || separator == payload.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(payload.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record Result(boolean validationError, String redirectUrl) {
        public static Result invalid() {
            return new Result(true, null);
        }

        public static Result redirect(String url) {
            return new Result(false, url);
        }
    }
}
