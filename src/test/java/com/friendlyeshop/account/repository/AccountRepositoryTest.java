package com.friendlyeshop.account.repository;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.model.Account;
import java.time.Instant;
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
class AccountRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void createsFindsByGoogleSubjectAndUpdatesWithoutDuplicating() {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        Account created = Account.open("google-sub-1", "ada@example.com", "Ada", now);

        accountRepository.save(created);

        Account found = accountRepository.findByGoogleSub("google-sub-1").orElseThrow();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo("ada@example.com");
        assertThat(found.getDisplayName()).isEqualTo("Ada");

        found.updateProfile("ada.new@example.com", "Ada Lovelace", now.plusSeconds(30));
        accountRepository.save(found);

        assertThat(accountRepository.count()).isEqualTo(1);
        Account updated = accountRepository.findByGoogleSub("google-sub-1").orElseThrow();
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getEmail()).isEqualTo("ada.new@example.com");
        assertThat(updated.getDisplayName()).isEqualTo("Ada Lovelace");
    }
}
