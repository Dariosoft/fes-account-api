package com.friendlyeshop.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account.google")
public record GoogleAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String issuer
) {

    public GoogleAuthProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "https://accounts.google.com";
        }
    }
}
