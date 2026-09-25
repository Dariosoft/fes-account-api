package com.friendlyeshop.account;

public record AuthenticatedAccountResponse(
        String displayName,
        String email
) {
}
