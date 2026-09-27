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

class CompleteGoogleLoginUpsertTest {
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
    void firstLoginCreatesAccount() {
        google.succeedWith(new GoogleAuthClient.GoogleIdentity("sub-1", "a@example.com", "Ada"));
        String state = StartGoogleLogin.encodeState("http://store.example.test");

        CompleteGoogleLogin.Result result = useCase.execute("code", state);

        assertThat(result.success()).isTrue();
        assertThat(accounts.bySubject).hasSize(1);
        assertThat(accounts.findByGoogleSubject("sub-1")).isPresent().get()
                .extracting(Account::email, Account::displayName)
                .containsExactly("a@example.com", "Ada");
    }

    @Test
    void secondLoginReusesSameAccountId() {
        google.succeedWith(new GoogleAuthClient.GoogleIdentity("sub-1", "a@example.com", "Ada"));
        String state = StartGoogleLogin.encodeState("http://store.example.test");
        UUID firstId = useCase.execute("code", state).sessionId();
        Account first = accounts.findByGoogleSubject("sub-1").orElseThrow();

        UUID secondSession = useCase.execute("code", state).sessionId();
        Account second = accounts.findByGoogleSubject("sub-1").orElseThrow();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(accounts.bySubject).hasSize(1);
        assertThat(secondSession).isNotEqualTo(firstId);
    }

    @Test
    void updatesEmailAndNameAndAcceptsAnyGoogleSubject() {
        google.succeedWith(new GoogleAuthClient.GoogleIdentity("any-sub", "old@example.com", "Old"));
        String state = StartGoogleLogin.encodeState("http://store.example.test");
        useCase.execute("code", state);

        google.succeedWith(new GoogleAuthClient.GoogleIdentity("any-sub", "new@example.com", "New"));
        useCase.execute("code", state);

        Account account = accounts.findByGoogleSubject("any-sub").orElseThrow();
        assertThat(account.email()).isEqualTo("new@example.com");
        assertThat(account.displayName()).isEqualTo("New");
    }

    private static final class InMemoryAccounts implements AccountRepository {
        final Map<String, Account> bySubject = new HashMap<>();

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
        private final Map<UUID, Session> byId = new HashMap<>();

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
