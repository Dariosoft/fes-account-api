package com.friendlyeshop.account.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.account.application.AccountRepository;
import com.friendlyeshop.account.account.domain.Account;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetSessionTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    void returnsUnauthenticatedWhenCookieAbsentOrInvalid() {
        GetSession useCase = new GetSession(new StubSessions(), new StubAccounts());

        assertThat(useCase.execute(null).authenticated()).isFalse();
        assertThat(useCase.execute(UUID.randomUUID()).authenticated()).isFalse();
    }

    @Test
    void returnsAccountWhenSessionValid() {
        Account account = Account.create("sub", "ada@example.com", "Ada", NOW);
        Session session = Session.open(account.id(), NOW);
        StubAccounts accounts = new StubAccounts();
        accounts.save(account);
        StubSessions sessions = new StubSessions();
        sessions.save(session);

        SessionView view = new GetSession(sessions, accounts).execute(session.id());

        assertThat(view.authenticated()).isTrue();
        assertThat(view.id()).isEqualTo(account.id());
        assertThat(view.email()).isEqualTo("ada@example.com");
        assertThat(view.name()).isEqualTo("Ada");
    }

    private static final class StubAccounts implements AccountRepository {
        private final Map<UUID, Account> byId = new HashMap<>();

        private final Map<String, Account> bySubject = new HashMap<>();

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
            byId.put(account.id(), account);
            bySubject.put(account.googleSubject(), account);
            return account;
        }
    }

    private static final class StubSessions implements SessionRepository {
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
