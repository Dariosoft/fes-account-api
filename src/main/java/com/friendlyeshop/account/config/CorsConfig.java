package com.friendlyeshop.account.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final AuthProperties authProperties;

    public CorsConfig(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = authProperties.getBrowserOrigins();
        registry.addMapping("/accounts/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
