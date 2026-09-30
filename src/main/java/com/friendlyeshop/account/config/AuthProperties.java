package com.friendlyeshop.account.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ToString(onlyExplicitlyIncluded = true)
@ConfigurationProperties(prefix = "fes.auth")
public class AuthProperties {
    @NotBlank
    @ToString.Include
    private String googleClientId = "";

    @NotBlank
    private String googleClientSecret = "";

    @NotBlank
    @ToString.Include
    private String sessionCookieDomain = "";

    @NotEmpty
    @ToString.Include
    private List<String> browserOrigins = List.of();

    @ToString.Include
    private boolean cookieSecure;

    @NotBlank
    @ToString.Include
    private String publicApiBaseUrl = "";

    @ToString.Include(name = "googleClientSecret")
    String maskedGoogleClientSecret() {
        return "***";
    }
}
