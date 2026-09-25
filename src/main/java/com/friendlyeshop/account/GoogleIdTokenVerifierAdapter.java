package com.friendlyeshop.account;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleIdTokenVerifierAdapter(GoogleAuthProperties properties) {
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance());
        if (properties.clientId() != null && !properties.clientId().isBlank()) {
            builder.setAudience(Collections.singletonList(properties.clientId()));
        }
        if (properties.issuer() != null && !properties.issuer().isBlank()) {
            builder.setIssuer(properties.issuer());
        }
        this.verifier = builder.build();
    }

    GoogleIdTokenVerifierAdapter(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new GoogleAuthenticationException("No se pudo validar la identidad con Google.");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new GoogleAuthenticationException("Google no proporcionó un correo válido.");
            }
            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email;
            }
            return new GoogleIdentity(email, name);
        } catch (GoogleAuthenticationException ex) {
            throw ex;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
            throw new GoogleAuthenticationException("No se pudo validar la identidad con Google.", ex);
        }
    }
}
