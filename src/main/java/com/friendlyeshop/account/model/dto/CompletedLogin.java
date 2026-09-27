package com.friendlyeshop.account.model.dto;

import java.util.UUID;

public record CompletedLogin(boolean success, String redirectUrl, UUID sessionId) {
    public static CompletedLogin opened(String redirectUrl, UUID sessionId) {
        return new CompletedLogin(true, redirectUrl, sessionId);
    }

    public static CompletedLogin failed(String redirectUrl) {
        return new CompletedLogin(false, redirectUrl, null);
    }
}
