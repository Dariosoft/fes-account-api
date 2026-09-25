package com.friendlyeshop.account;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SessionCookieProperties.class, GoogleAuthProperties.class})
public class AccountConfiguration {
}
