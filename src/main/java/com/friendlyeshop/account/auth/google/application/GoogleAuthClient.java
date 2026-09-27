package com.friendlyeshop.account.auth.google.application;

import java.util.Optional;

public interface GoogleAuthClient {
    String authorizationUrl(String state);

    Optional<GoogleIdentity> exchangeCode(String code);

    record GoogleIdentity(String subject, String email, String name) {
    }
}
