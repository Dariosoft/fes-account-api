package com.friendlyeshop.account.session.application;

import java.util.UUID;

public record SessionView(boolean authenticated, UUID id, String email, String name) {
    public static SessionView unauthenticated() {
        return new SessionView(false, null, null, null);
    }

    public static SessionView authenticated(UUID id, String email, String name) {
        return new SessionView(true, id, email, name);
    }
}
