package com.friendlyeshop.account.model;

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

        assertThat(session.getId()).isNotNull();
        assertThat(session.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(session.getCreatedAt()).isEqualTo(NOW);
        assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(30, ChronoUnit.DAYS));
        assertThat(session.getRevokedAt()).isNull();
        assertThat(session.isValid(NOW)).isTrue();
        assertThat(session.isValid(NOW.plus(30, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    void revokeInvalidatesSession() {
        Session session = Session.open(ACCOUNT_ID, NOW);

        session.revoke(NOW.plusSeconds(10));

        assertThat(session.isValid(NOW.plusSeconds(11))).isFalse();
        assertThat(session.getRevokedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void newSessionDoesNotInvalidateAnotherForSameAccount() {
        Session first = Session.open(ACCOUNT_ID, NOW);
        Session second = Session.open(ACCOUNT_ID, NOW.plusSeconds(1));

        assertThat(first.isValid(NOW.plusSeconds(2))).isTrue();
        assertThat(second.isValid(NOW.plusSeconds(2))).isTrue();
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getAccountId()).isEqualTo(second.getAccountId());
    }
}
