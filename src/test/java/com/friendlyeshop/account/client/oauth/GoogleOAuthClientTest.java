package com.friendlyeshop.account.client.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.config.AuthProperties;
import com.friendlyeshop.account.model.dto.GoogleProfile;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoogleOAuthClientTest {
    @Test
    void authorizationUrlUsesConfiguredCallbackAndState() {
        AuthProperties properties = properties();
        GoogleOAuthClient client = new GoogleOAuthClient(properties, (code, redirectUri) -> Optional.empty());

        String url = client.authorizationUrl("opaque-state");

        assertThat(url).contains("accounts.google.com");
        assertThat(url).contains("state=opaque-state");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("openid");
        assertThat(properties.getGoogleClientSecret()).isEqualTo("test-client-secret");
        assertThat(url).doesNotContain("test-client-secret");
    }

    @Test
    void mapsVerifiedPayloadToIdentity() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub");
        payload.set("email", "ada@example.com");
        payload.set("name", "Ada");

        GoogleProfile identity = HttpTokenExchange.toIdentity(payload);

        assertThat(identity.subject()).isEqualTo("google-sub");
        assertThat(identity.email()).isEqualTo("ada@example.com");
        assertThat(identity.name()).isEqualTo("Ada");
    }

    @Test
    void exchangeUsesTokenExchangeCollaborator() throws Exception {
        GoogleOAuthClient client = new GoogleOAuthClient(
                properties(),
                (code, redirectUri) -> {
                    assertThat(code).isEqualTo("auth-code");
                    assertThat(redirectUri).isEqualTo("http://api.example.test/accounts/login/google/callback");
                    return Optional.of(new GoogleProfile("sub", "a@example.com", "Ada"));
                });

        Optional<GoogleProfile> profile = client.exchangeCode("auth-code");
        assertThat(profile).isPresent();
        assertThat(profile.orElseThrow().subject()).isEqualTo("sub");
        assertThat(client.exchangeCode("")).isEmpty();
    }

    private AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.setGoogleClientId("test-client-id");
        properties.setGoogleClientSecret("test-client-secret");
        properties.setSessionCookieDomain(".example.test");
        properties.setBrowserOrigins(List.of("http://store.example.test"));
        properties.setPublicApiBaseUrl("http://api.example.test");
        return properties;
    }
}
