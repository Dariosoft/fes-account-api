package com.friendlyeshop.account.client.oauth;

import com.friendlyeshop.account.config.AuthProperties;
import com.friendlyeshop.account.model.dto.GoogleProfile;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthClient.class);

    private static final List<String> SCOPES = List.of("openid", "email", "profile");

    private final AuthProperties authProperties;

    private final TokenExchange tokenExchange;

    @Autowired
    public GoogleOAuthClient(AuthProperties authProperties) {
        this(authProperties, new HttpTokenExchange(authProperties));
    }

    GoogleOAuthClient(AuthProperties authProperties, TokenExchange tokenExchange) {
        this.authProperties = authProperties;
        this.tokenExchange = tokenExchange;
    }

    public String authorizationUrl(String state) {
        return new GoogleAuthorizationCodeRequestUrl(
                authProperties.getGoogleClientId(),
                callbackUri(),
                SCOPES)
                .setState(state)
                .build();
    }

    public Optional<GoogleProfile> exchangeCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try {
            return tokenExchange.exchange(code, callbackUri());
        } catch (IOException | GeneralSecurityException ex) {
            LOGGER.warn("Google token exchange failed");
            return Optional.empty();
        }
    }

    private String callbackUri() {
        String base = authProperties.getPublicApiBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/accounts/login/google/callback";
    }

}
