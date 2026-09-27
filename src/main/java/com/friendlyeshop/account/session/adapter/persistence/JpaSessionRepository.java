package com.friendlyeshop.account.session.adapter.persistence;

import com.friendlyeshop.account.session.application.SessionRepository;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaSessionRepository implements SessionRepository {
    private final SessionJpaRepository jpaRepository;

    private final Clock clock;

    public JpaSessionRepository(SessionJpaRepository jpaRepository, Clock clock) {
        this.jpaRepository = jpaRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Session save(Session session) {
        SessionJpaEntity entity = new SessionJpaEntity(
                session.id(),
                session.accountId(),
                session.createdAt(),
                session.expiresAt(),
                session.revokedAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> findValidById(UUID sessionId) {
        return jpaRepository.findValidById(sessionId, Instant.now(clock)).map(this::toDomain);
    }

    @Override
    @Transactional
    public void invalidate(UUID sessionId) {
        jpaRepository.findById(sessionId).ifPresent(entity -> {
            if (entity.getRevokedAt() == null) {
                entity.setRevokedAt(Instant.now(clock));
                jpaRepository.save(entity);
            }
        });
    }

    private Session toDomain(SessionJpaEntity entity) {
        return Session.reconstitute(
                entity.getId(),
                entity.getAccountId(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt());
    }
}
