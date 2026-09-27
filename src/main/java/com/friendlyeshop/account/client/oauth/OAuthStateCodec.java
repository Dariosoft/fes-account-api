package com.friendlyeshop.account.client.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public final class OAuthStateCodec {
    private OAuthStateCodec() {
    }

    public static String encode(String returnTo) {
        String payload = UUID.randomUUID() + "|" + returnTo;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<String> returnTo(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            int separator = payload.indexOf('|');
            if (separator < 0 || separator == payload.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(payload.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
