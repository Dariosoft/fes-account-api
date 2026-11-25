package com.friendlyeshop.account.client.oauth;

import com.friendlyeshop.account.model.dto.in.GoogleProfileDetails;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@FunctionalInterface
interface TokenExchange {
    Optional<GoogleProfileDetails> exchange(String code, String redirectUri)
            throws IOException, GeneralSecurityException;
}
