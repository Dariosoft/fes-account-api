package com.friendlyeshop.account.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogoutTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    void invalidatesWhenSessionIdPresentAndAlwaysClearsCookie() {
        Session session = Session.open(UUID.randomUUID(), NOW);
        StubSessions sessions = new StubSessions();
        sessions.save(session);

        Logout.LogoutResult result = new Logout(sessions).execute(session.id());

        assertThat(result.shouldClearCookie()).isTrue();
        assertThat(sessions.findValidById(session.id())).isEmpty();
    }

    @Test
    void succeedsWithoutSessionIdAndStillClearsCookie() {
        StubSessions sessions = new StubSessions();

        Logout.LogoutResult result = new Logout(sessions).execute(null);

        assertThat(result.shouldClearCookie()).isTrue();
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
