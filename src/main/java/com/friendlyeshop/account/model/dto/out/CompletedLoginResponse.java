package com.friendlyeshop.account.model.dto.out;

import java.util.UUID;

public record CompletedLoginResponse(boolean success, String redirectUrl, UUID sessionId) {
    public static CompletedLoginResponse opened(String redirectUrl, UUID sessionId) {
        return new CompletedLoginResponse(true, redirectUrl, sessionId);
    }

    public static CompletedLoginResponse failed(String redirectUrl) {
        return new CompletedLoginResponse(false, redirectUrl, null);
    }
}
