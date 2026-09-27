package com.friendlyeshop.account.session.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.account.adapter.persistence.JpaAccountRepository;
import com.friendlyeshop.account.account.domain.Account;
import com.friendlyeshop.account.config.TimeConfig;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({JpaSessionRepository.class, JpaAccountRepository.class, TimeConfig.class})
class JpaSessionRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaSessionRepository sessionRepository;

    @Autowired
    private JpaAccountRepository accountRepository;

    private UUID accountId;

    @BeforeEach
    void seedAccount() {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        Account account = Account.create("session-sub", "session@example.com", "Session User", now);
        accountId = accountRepository.save(account).id();
    }

    @Test
    void createsTwoSessionsInvalidatesOneAndKeepsTheOtherValid() {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        Session first = Session.open(accountId, now);
        Session second = Session.open(accountId, now.plusSeconds(1));

        sessionRepository.save(first);
        sessionRepository.save(second);

        sessionRepository.invalidate(first.id());

        assertThat(sessionRepository.findValidById(first.id())).isEmpty();
        assertThat(sessionRepository.findValidById(second.id())).isPresent().get()
                .extracting(Session::id, Session::accountId)
                .containsExactly(second.id(), accountId);
    }
}
