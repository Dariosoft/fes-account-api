package com.friendlyeshop.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "sessions")
public class Session {
    public static final int DEFAULT_TTL_DAYS = 30;

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Session() {
    }

    public static Session open(UUID accountId, Instant now) {
        Instant createdAt = Objects.requireNonNull(now, "now");
        Session session = new Session();
        session.id = UUID.randomUUID();
        session.accountId = Objects.requireNonNull(accountId, "accountId");
        session.createdAt = createdAt;
        session.expiresAt = createdAt.plus(DEFAULT_TTL_DAYS, ChronoUnit.DAYS);
        return session;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = Objects.requireNonNull(now, "now");
        }
    }

    public boolean isValid(Instant now) {
        Instant at = Objects.requireNonNull(now, "now");
        return revokedAt == null && at.isBefore(expiresAt);
    }

}
