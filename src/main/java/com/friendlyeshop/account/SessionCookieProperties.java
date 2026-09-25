package com.friendlyeshop.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "account.session.cookie")
public record SessionCookieProperties(
        boolean secure,
        String sameSite
) {

    public SessionCookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
    }
}
