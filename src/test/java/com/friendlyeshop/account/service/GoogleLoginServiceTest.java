package com.friendlyeshop.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.friendlyeshop.account.client.oauth.GoogleOAuthClient;
import com.friendlyeshop.account.client.oauth.OAuthStateCodec;
import com.friendlyeshop.account.config.AuthProperties;
import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.Session;
import com.friendlyeshop.account.model.dto.CompletedLogin;
import com.friendlyeshop.account.model.dto.GoogleProfile;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.SessionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleLoginServiceTest {
    private static final String STORE = "http://store.example.test";

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SessionRepository sessionRepository;

    private final Map<String, Account> accountsBySubject = new HashMap<>();

    private final Map<UUID, Session> sessionsById = new HashMap<>();

    private GoogleLoginService service;

    @BeforeEach
    void setUp() {
        service = new GoogleLoginService(properties(), googleOAuthClient, accountRepository, sessionRepository);
    }

    @Test
    void allowedOriginReturnsGoogleUrlCarryingTheReturnTo() {
        when(googleOAuthClient.authorizationUrl(any())).thenAnswer(invocation -> {
            String state = invocation.getArgument(0);
            return "https://accounts.google.com/o/oauth2/v2/auth?state=" + state;
        });

        Optional<String> redirectUrl = service.start(STORE);

        assertThat(redirectUrl).isPresent();
        assertThat(redirectUrl.get()).startsWith("https://accounts.google.com/");
        String state = redirectUrl.get().substring(redirectUrl.get().indexOf("state=") + "state=".length());
        assertThat(OAuthStateCodec.returnTo(state)).contains(STORE);
    }

    @Test
    void missingOrForeignOriginDoesNotCallGoogle() {
        assertThat(service.start(null)).isEmpty();
        assertThat(service.start("http://evil.example")).isEmpty();
        verify(googleOAuthClient, never()).authorizationUrl(any());
    }

    @Test
    void firstLoginCreatesAccountAndLaterLoginReusesItWhileUpdatingProfile() {
        rememberSaves();
        when(googleOAuthClient.exchangeCode("code"))
                .thenReturn(Optional.of(new GoogleProfile("sub-1", "a@example.com", "Ada")));
        String state = OAuthStateCodec.encode(STORE);

        CompletedLogin first = service.complete("code", state);
        when(googleOAuthClient.exchangeCode("code"))
                .thenReturn(Optional.of(new GoogleProfile("sub-1", "new@example.com", "New")));
        CompletedLogin second = service.complete("code", state);

        assertThat(first.success()).isTrue();
        assertThat(second.success()).isTrue();
        assertThat(second.redirectUrl()).isEqualTo(STORE);
        assertThat(accountsBySubject).hasSize(1);
        Account account = accountsBySubject.get("sub-1");
        assertThat(account.getEmail()).isEqualTo("new@example.com");
        assertThat(account.getDisplayName()).isEqualTo("New");
        assertThat(sessionsById.get(first.sessionId()).isValid(account.getCreatedAt().plusSeconds(5))).isTrue();
        assertThat(sessionsById.get(second.sessionId()).isValid(account.getCreatedAt().plusSeconds(5))).isTrue();
        assertThat(first.sessionId()).isNotEqualTo(second.sessionId());
    }

    @Test
    void failedGoogleExchangeRedirectsWithLoginErrorAndCreatesNothing() {
        when(googleOAuthClient.exchangeCode("cancel")).thenReturn(Optional.empty());

        CompletedLogin result = service.complete("cancel", OAuthStateCodec.encode(STORE));

        assertThat(result.success()).isFalse();
        assertThat(result.sessionId()).isNull();
        assertThat(result.redirectUrl()).startsWith(STORE);
        assertThat(result.redirectUrl()).contains("login_error=1");
        verify(accountRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void invalidStateFailsWithoutOpeningASession() {
        CompletedLogin result = service.complete("code", "not-a-state");

        assertThat(result.success()).isFalse();
        assertThat(result.redirectUrl()).isEqualTo("http://localhost");
        verify(sessionRepository, never()).save(any());
    }

    private void rememberSaves() {
        when(accountRepository.findByGoogleSub(any())).thenAnswer(invocation ->
                Optional.ofNullable(accountsBySubject.get(invocation.getArgument(0))));
        when(accountRepository.save(any())).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            accountsBySubject.put(account.getGoogleSub(), account);
            return account;
        });
        when(sessionRepository.save(any())).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            sessionsById.put(session.getId(), session);
            return session;
        });
    }

    private static AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.setGoogleClientId("id");
        properties.setGoogleClientSecret("secret");
        properties.setSessionCookieDomain(".example.test");
        properties.setBrowserOrigins(List.of(STORE, "http://panel.example.test"));
        properties.setPublicApiBaseUrl("http://api.example.test");
        return properties;
    }
}
