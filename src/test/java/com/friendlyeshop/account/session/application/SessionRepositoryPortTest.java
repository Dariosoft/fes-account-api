package com.friendlyeshop.account.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionRepositoryPortTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    void stubCanCreateFindValidAndInvalidate() {
        InMemorySessionRepository repository = new InMemorySessionRepository();
        Session session = Session.open(UUID.randomUUID(), NOW);

        repository.save(session);
        assertThat(repository.findValidById(session.id())).isPresent().get()
                .extracting(Session::id)
                .isEqualTo(session.id());

        repository.invalidate(session.id());
        assertThat(repository.findValidById(session.id())).isEmpty();
    }

    private static final class InMemorySessionRepository implements SessionRepository {
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
