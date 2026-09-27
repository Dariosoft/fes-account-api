package com.friendlyeshop.account.session.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void openCreatesSessionExpiringInThirtyDays() {
        Session session = Session.open(ACCOUNT_ID, NOW);

        assertThat(session.id()).isNotNull();
        assertThat(session.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(session.createdAt()).isEqualTo(NOW);
        assertThat(session.expiresAt()).isEqualTo(NOW.plus(30, ChronoUnit.DAYS));
        assertThat(session.revokedAt()).isNull();
        assertThat(session.isValid(NOW)).isTrue();
        assertThat(session.isValid(NOW.plus(30, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    void revokeInvalidatesSession() {
        Session session = Session.open(ACCOUNT_ID, NOW);

        session.revoke(NOW.plusSeconds(10));

        assertThat(session.isValid(NOW.plusSeconds(11))).isFalse();
        assertThat(session.revokedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void newSessionDoesNotInvalidateAnotherForSameAccount() {
        Session first = Session.open(ACCOUNT_ID, NOW);
        Session second = Session.open(ACCOUNT_ID, NOW.plusSeconds(1));

        assertThat(first.isValid(NOW.plusSeconds(2))).isTrue();
        assertThat(second.isValid(NOW.plusSeconds(2))).isTrue();
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.accountId()).isEqualTo(second.accountId());
    }
}
