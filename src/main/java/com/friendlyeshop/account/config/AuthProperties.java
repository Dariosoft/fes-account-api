package com.friendlyeshop.account.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fes.auth")
public class AuthProperties {
    @NotBlank
    private String googleClientId = "";

    @NotBlank
    private String googleClientSecret = "";

    @NotBlank
    private String sessionCookieDomain = "";

    @NotEmpty
    private List<String> browserOrigins = List.of();

    private boolean cookieSecure;

    @NotBlank
    private String publicApiBaseUrl = "";

    public String getGoogleClientId() {
        return googleClientId;
    }

    public void setGoogleClientId(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public String getGoogleClientSecret() {
        return googleClientSecret;
    }

    public void setGoogleClientSecret(String googleClientSecret) {
        this.googleClientSecret = googleClientSecret;
    }

    public String getSessionCookieDomain() {
        return sessionCookieDomain;
    }

    public void setSessionCookieDomain(String sessionCookieDomain) {
        this.sessionCookieDomain = sessionCookieDomain;
    }

    public List<String> getBrowserOrigins() {
        return browserOrigins;
    }

    public void setBrowserOrigins(List<String> browserOrigins) {
        this.browserOrigins = browserOrigins;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getPublicApiBaseUrl() {
        return publicApiBaseUrl;
    }

    public void setPublicApiBaseUrl(String publicApiBaseUrl) {
        this.publicApiBaseUrl = publicApiBaseUrl;
    }

    @Override
    public String toString() {
        return "AuthProperties{googleClientId='" + googleClientId
                + "', googleClientSecret='***', sessionCookieDomain='" + sessionCookieDomain
                + "', browserOrigins=" + browserOrigins
                + ", cookieSecure=" + cookieSecure
                + ", publicApiBaseUrl='" + publicApiBaseUrl + "'}";
    }
}
