package com.friendlyeshop.account.auth.google.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.account.application.AccountRepository;
import com.friendlyeshop.account.account.domain.Account;
import com.friendlyeshop.account.session.application.SessionRepository;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteGoogleLoginSessionTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    private FakeGoogleAuthClient google;

    private InMemoryAccounts accounts;

    private InMemorySessions sessions;

    private CompleteGoogleLogin useCase;

    @BeforeEach
    void setUp() {
        google = new FakeGoogleAuthClient();
        accounts = new InMemoryAccounts();
        sessions = new InMemorySessions();
        useCase = new CompleteGoogleLogin(google, accounts, sessions, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void successCreatesNewSessionWithoutInvalidatingPreviousAndRedirects() {
        google.succeedWith(new GoogleAuthClient.GoogleIdentity("sub", "a@example.com", "Ada"));
        String state = StartGoogleLogin.encodeState("http://store.example.test");
        CompleteGoogleLogin.Result first = useCase.execute("code", state);
        CompleteGoogleLogin.Result second = useCase.execute("code", state);

        assertThat(first.success()).isTrue();
        assertThat(second.success()).isTrue();
        assertThat(second.redirectUrl()).isEqualTo("http://store.example.test");
        assertThat(sessions.findValidById(first.sessionId())).isPresent();
        assertThat(sessions.findValidById(second.sessionId())).isPresent();
        assertThat(first.sessionId()).isNotEqualTo(second.sessionId());
    }

    @Test
    void failureRedirectsWithLoginErrorWithoutCreatingSession() {
        google.failNextExchange();
        String state = StartGoogleLogin.encodeState("http://store.example.test");

        CompleteGoogleLogin.Result result = useCase.execute("cancel", state);

        assertThat(result.success()).isFalse();
        assertThat(result.sessionId()).isNull();
        assertThat(result.redirectUrl()).contains("login_error=1");
        assertThat(result.redirectUrl()).startsWith("http://store.example.test");
        assertThat(sessions.byId).isEmpty();
    }

    private static final class InMemoryAccounts implements AccountRepository {
        private final Map<String, Account> bySubject = new HashMap<>();

        private final Map<UUID, Account> byId = new HashMap<>();

        @Override
        public Optional<Account> findByGoogleSubject(String googleSubject) {
            return Optional.ofNullable(bySubject.get(googleSubject));
        }

        @Override
        public Optional<Account> findById(UUID accountId) {
            return Optional.ofNullable(byId.get(accountId));
        }

        @Override
        public Account save(Account account) {
            bySubject.put(account.googleSubject(), account);
            byId.put(account.id(), account);
            return account;
        }
    }

    private static final class InMemorySessions implements SessionRepository {
        final Map<UUID, Session> byId = new HashMap<>();

        @Override
        public Session save(Session session) {
            byId.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<Session> findValidById(UUID sessionId) {
            return Optional.ofNullable(byId.get(sessionId)).filter(session -> session.isValid(NOW));
        }

        @Override
        public void invalidate(UUID sessionId) {
            Session session = byId.get(sessionId);
            if (session != null) {
                session.revoke(NOW);
            }
        }
    }
}
