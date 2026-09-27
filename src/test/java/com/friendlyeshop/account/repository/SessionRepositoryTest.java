package com.friendlyeshop.account.repository;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.Session;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class SessionRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private UUID accountId;

    @BeforeEach
    void seedAccount() {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        Account account = Account.open("session-sub", "session@example.com", "Session User", now);
        accountId = accountRepository.save(account).getId();
    }

    @Test
    void createsTwoSessionsAndRevokingOneLeavesTheOtherValid() {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        Session first = sessionRepository.save(Session.open(accountId, now));
        Session second = sessionRepository.save(Session.open(accountId, now.plusSeconds(1)));

        first.revoke(now.plusSeconds(2));
        sessionRepository.save(first);

        Session revoked = sessionRepository.findById(first.getId()).orElseThrow();
        Session kept = sessionRepository.findById(second.getId()).orElseThrow();
        assertThat(revoked.isValid(now.plusSeconds(3))).isFalse();
        assertThat(kept.isValid(now.plusSeconds(3))).isTrue();
        assertThat(kept.getAccountId()).isEqualTo(accountId);
    }
}
