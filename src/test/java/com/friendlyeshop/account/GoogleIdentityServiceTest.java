package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class GoogleIdentityServiceTest {

    @Test
    void validTokenReturnsEmailAndName() {
        GoogleIdentityService service = new GoogleIdentityService(
                token -> new GoogleIdentity("ada@example.com", "Ada Lovelace"));

        GoogleIdentity identity = service.verifyIdToken("valid-token");

        assertThat(identity.email()).isEqualTo("ada@example.com");
        assertThat(identity.displayName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void invalidOrRejectedTokenThrowsSpanishError() {
        GoogleIdentityService service = new GoogleIdentityService(token -> {
            throw new GoogleAuthenticationException("No se pudo validar la identidad con Google.");
        });

        assertThatThrownBy(() -> service.verifyIdToken("bad-token"))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessage("No se pudo validar la identidad con Google.");
    }

    @Test
    void blankCredentialThrowsSpanishErrorWithoutInventingIdentity() {
        GoogleIdentityService service = new GoogleIdentityService(token -> {
            throw new AssertionError("verifier must not be called");
        });

        assertThatThrownBy(() -> service.verifyIdToken(" "))
                .isInstanceOf(GoogleAuthenticationException.class)
                .hasMessage("La credencial de Google es inválida o fue rechazada.");
    }
}
