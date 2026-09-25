package com.friendlyeshop.account;

import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityService {

    private final GoogleTokenVerifier tokenVerifier;

    public GoogleIdentityService(GoogleTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    public GoogleIdentity verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new GoogleAuthenticationException("La credencial de Google es inválida o fue rechazada.");
        }
        return tokenVerifier.verify(idToken);
    }
}
