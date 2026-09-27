package com.friendlyeshop.account.session.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared browser session. Multiple valid sessions per account are allowed; a new login does not
 * revoke prior ones.
 */
public final class Session {
    public static final int DEFAULT_TTL_DAYS = 30;

    private final UUID id;

    private final UUID accountId;

    private final Instant createdAt;

    private final Instant expiresAt;

    private Instant revokedAt;

    private Session(UUID id, UUID accountId, Instant createdAt, Instant expiresAt, Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revokedAt = revokedAt;
    }

    public static Session open(UUID accountId, Instant now) {
        Instant createdAt = Objects.requireNonNull(now, "now");
        Instant expiresAt = createdAt.plus(DEFAULT_TTL_DAYS, ChronoUnit.DAYS);
        return new Session(UUID.randomUUID(), accountId, createdAt, expiresAt, null);
    }

    public static Session reconstitute(
            UUID id,
            UUID accountId,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt) {
        return new Session(id, accountId, createdAt, expiresAt, revokedAt);
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

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
