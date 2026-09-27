package com.friendlyeshop.account.session.application;

import com.friendlyeshop.account.session.domain.Session;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    Session save(Session session);

    Optional<Session> findValidById(UUID sessionId);

    void invalidate(UUID sessionId);
}
