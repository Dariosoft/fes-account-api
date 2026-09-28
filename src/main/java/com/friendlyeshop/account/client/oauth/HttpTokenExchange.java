package com.friendlyeshop.account.client.oauth;

import com.friendlyeshop.account.config.AuthProperties;
import com.friendlyeshop.account.constants.GoogleClaims;
import com.friendlyeshop.account.model.dto.GoogleProfile;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

final class HttpTokenExchange implements TokenExchange {
    private final AuthProperties authProperties;

    private final NetHttpTransport transport = new NetHttpTransport();

    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    HttpTokenExchange(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public Optional<GoogleProfile> exchange(String code, String redirectUri)
            throws IOException, GeneralSecurityException {
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                transport,
                jsonFactory,
                authProperties.getGoogleClientId(),
                authProperties.getGoogleClientSecret(),
                code,
                redirectUri)
                .execute();
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(List.of(authProperties.getGoogleClientId()))
                .build();
        GoogleIdToken idToken = verifier.verify(tokenResponse.getIdToken());
        if (idToken == null) {
            return Optional.empty();
        }
        return Optional.of(toIdentity(idToken.getPayload()));
    }

    static GoogleProfile toIdentity(GoogleIdToken.Payload payload) {
        String email = (String) payload.get(GoogleClaims.EMAIL);
        String name = (String) payload.get(GoogleClaims.NAME);
        if (name == null || name.isBlank()) {
            name = email;
        }
        return new GoogleProfile(payload.getSubject(), email, name);
    }
}
