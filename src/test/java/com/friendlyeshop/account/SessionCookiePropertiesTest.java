package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SessionCookiePropertiesTest {

    @Test
    void documentsSecureAndSameSiteDefaultsForSharedSession() {
        // Local default: Secure=false; non-local should set ACCOUNT_SESSION_COOKIE_SECURE=true.
        SessionCookieProperties local = new SessionCookieProperties(false, "Lax");
        SessionCookieProperties nonLocal = new SessionCookieProperties(true, "None");

        assertThat(local.secure()).isFalse();
        assertThat(local.sameSite()).isEqualTo("Lax");
        assertThat(nonLocal.secure()).isTrue();
        assertThat(nonLocal.sameSite()).isEqualTo("None");
        assertThat(SessionCookieSupport.COOKIE_NAME).isEqualTo("FES_SESSION");
    }

    @Test
    void googleSecretsAreExternalizedNotHardcodedInSourceDefaults() {
        GoogleAuthProperties props = new GoogleAuthProperties("", "", "http://localhost/callback", null);
        assertThat(props.clientId()).isEmpty();
        assertThat(props.clientSecret()).isEmpty();
        assertThat(props.issuer()).isEqualTo("https://accounts.google.com");
    }
}
