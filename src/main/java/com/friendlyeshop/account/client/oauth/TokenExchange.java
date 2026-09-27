package com.friendlyeshop.account.client.oauth;

import com.friendlyeshop.account.model.dto.GoogleProfile;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@FunctionalInterface
interface TokenExchange {
    Optional<GoogleProfile> exchange(String code, String redirectUri)
            throws IOException, GeneralSecurityException;
}
