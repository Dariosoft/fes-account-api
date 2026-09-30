package com.friendlyeshop.account.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AuthPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsFromEnvironmentStyleProperties() {
        contextRunner
                .withPropertyValues(
                        "fes.auth.google-client-id=client-id",
                        "fes.auth.google-client-secret=secret-value",
                        "fes.auth.session-cookie-domain=.example.test",
                        "fes.auth.browser-origins=http://store.example.test,http://panel.example.test",
                        "fes.auth.cookie-secure=false",
                        "fes.auth.public-api-base-url=http://api.example.test")
                .run(context -> {
                    AuthProperties properties = context.getBean(AuthProperties.class);
                    assertThat(properties.getGoogleClientId()).isEqualTo("client-id");
                    assertThat(properties.getGoogleClientSecret()).isEqualTo("secret-value");
                    assertThat(properties.getSessionCookieDomain()).isEqualTo(".example.test");
                    assertThat(properties.getBrowserOrigins())
                            .containsExactly("http://store.example.test", "http://panel.example.test");
                    assertThat(properties.isCookieSecure()).isFalse();
                    assertThat(properties.toString()).doesNotContain("secret-value");
                });
    }

    @Test
    void failsWhenBrowserOriginsMissing() {
        contextRunner
                .withPropertyValues(
                        "fes.auth.google-client-id=client-id",
                        "fes.auth.google-client-secret=secret-value",
                        "fes.auth.session-cookie-domain=.example.test")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class TestConfig {
    }
}
