package com.friendlyeshop.account.session.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {
    @Query("""
            select s from SessionJpaEntity s
            where s.id = :id
              and s.revokedAt is null
              and s.expiresAt > :now
            """)
    Optional<SessionJpaEntity> findValidById(@Param("id") UUID id, @Param("now") Instant now);
}
