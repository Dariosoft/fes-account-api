package com.friendlyeshop.account.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponse(boolean authenticated, UUID id, String email, String name) {
    public static SessionResponse anonymous() {
        return new SessionResponse(false, null, null, null);
    }

    public static SessionResponse authenticated(UUID id, String email, String name) {
        return new SessionResponse(true, id, email, name);
    }
}
