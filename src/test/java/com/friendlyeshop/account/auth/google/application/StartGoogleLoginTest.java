package com.friendlyeshop.account.auth.google.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.config.AuthProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class StartGoogleLoginTest {
    @Test
    void allowedOriginGeneratesGoogleUrl() {
        FakeGoogleAuthClient google = new FakeGoogleAuthClient();
        StartGoogleLogin useCase = new StartGoogleLogin(properties(), google);

        StartGoogleLogin.Result result = useCase.execute("http://store.example.test");

        assertThat(result.validationError()).isFalse();
        assertThat(result.redirectUrl()).startsWith("https://accounts.google.com/");
        assertThat(google.lastState()).isNotBlank();
        assertThat(StartGoogleLogin.decodeReturnTo(google.lastState()))
                .contains("http://store.example.test");
    }

    @Test
    void invalidOrMissingOriginDoesNotCallGoogle() {
        FakeGoogleAuthClient google = new FakeGoogleAuthClient();
        StartGoogleLogin useCase = new StartGoogleLogin(properties(), google);

        assertThat(useCase.execute(null).validationError()).isTrue();
        assertThat(useCase.execute("http://evil.example").validationError()).isTrue();
        assertThat(google.lastState()).isNull();
    }

    private AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.setGoogleClientId("id");
        properties.setGoogleClientSecret("secret");
        properties.setSessionCookieDomain(".example.test");
        properties.setBrowserOrigins(List.of("http://store.example.test", "http://panel.example.test"));
        return properties;
    }
}
