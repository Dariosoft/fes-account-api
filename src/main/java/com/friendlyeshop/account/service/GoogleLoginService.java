package com.friendlyeshop.account.service;

import com.friendlyeshop.account.client.oauth.GoogleOAuthClient;
import com.friendlyeshop.account.client.oauth.OAuthStateCodec;
import com.friendlyeshop.account.config.AuthProperties;
import com.friendlyeshop.account.constants.LoginRedirects;
import com.friendlyeshop.account.constants.OAuthParameters;
import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.Session;
import com.friendlyeshop.account.model.dto.CompletedLogin;
import com.friendlyeshop.account.model.dto.GoogleProfile;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.SessionRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleLoginService {
    private final AuthProperties authProperties;

    private final GoogleOAuthClient googleOAuthClient;

    private final AccountRepository accountRepository;

    private final SessionRepository sessionRepository;

    public GoogleLoginService(
            AuthProperties authProperties,
            GoogleOAuthClient googleOAuthClient,
            AccountRepository accountRepository,
            SessionRepository sessionRepository) {
        this.authProperties = authProperties;
        this.googleOAuthClient = googleOAuthClient;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
    }

    public Optional<String> start(String returnTo) {
        if (returnTo == null || returnTo.isBlank() || !isAllowedOrigin(returnTo)) {
            return Optional.empty();
        }
        return Optional.of(googleOAuthClient.authorizationUrl(OAuthStateCodec.encode(returnTo)));
    }

    @Transactional
    public CompletedLogin complete(String code, String state) {
        Optional<String> returnTo = OAuthStateCodec.returnTo(state);
        if (returnTo.isEmpty()) {
            return CompletedLogin.failed(LoginRedirects.FALLBACK_URL);
        }
        String destination = returnTo.get();
        Optional<GoogleProfile> profile = googleOAuthClient.exchangeCode(code);
        if (profile.isEmpty()) {
            return CompletedLogin.failed(withLoginError(destination));
        }
        Account account = saveAccount(profile.get());
        Session session = Session.open(account.getId(), Instant.now());
        sessionRepository.save(session);
        return CompletedLogin.opened(destination, session.getId());
    }

    private Account saveAccount(GoogleProfile profile) {
        Instant now = Instant.now();
        Optional<Account> existing = accountRepository.findByGoogleSub(profile.subject());
        if (existing.isPresent()) {
            Account account = existing.get();
            account.updateProfile(profile.email(), profile.name(), now);
            return accountRepository.save(account);
        }
        Account created = Account.open(profile.subject(), profile.email(), profile.name(), now);
        return accountRepository.save(created);
    }

    private boolean isAllowedOrigin(String returnTo) {
        return authProperties.getBrowserOrigins().stream().anyMatch(returnTo::equals);
    }

    private static String withLoginError(String returnTo) {
        return UriComponentsBuilder.fromUriString(returnTo)
                .queryParam(OAuthParameters.LOGIN_ERROR, OAuthParameters.LOGIN_ERROR_VALUE)
                .build(true)
                .toUriString();
    }
}
