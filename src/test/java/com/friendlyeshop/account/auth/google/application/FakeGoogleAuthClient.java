package com.friendlyeshop.account.auth.google.application;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class FakeGoogleAuthClient implements GoogleAuthClient {
    private final AtomicReference<Optional<GoogleIdentity>> nextExchange =
            new AtomicReference<>(Optional.empty());

    private String lastState;

    @Override
    public String authorizationUrl(String state) {
        this.lastState = state;
        return "https://accounts.google.com/o/oauth2/v2/auth?state=" + state;
    }

    @Override
    public Optional<GoogleIdentity> exchangeCode(String code) {
        if ("cancel".equals(code) || code == null || code.isBlank()) {
            return Optional.empty();
        }
        return nextExchange.get();
    }

    public void succeedWith(GoogleIdentity identity) {
        nextExchange.set(Optional.of(identity));
    }

    public void failNextExchange() {
        nextExchange.set(Optional.empty());
    }

    public String lastState() {
        return lastState;
    }
}
