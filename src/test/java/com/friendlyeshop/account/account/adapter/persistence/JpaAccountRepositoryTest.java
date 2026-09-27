package com.friendlyeshop.account.account.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.account.domain.Account;
import java.time.Instant;
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
@Import(JpaAccountRepository.class)
class JpaAccountRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaAccountRepository accountRepository;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Test
    void createsFindsByGoogleSubjectAndUpdatesWithoutDuplicating() {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        Account created = Account.create("google-sub-1", "ada@example.com", "Ada", now);

        accountRepository.save(created);

        Account found = accountRepository.findByGoogleSubject("google-sub-1").orElseThrow();
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.email()).isEqualTo("ada@example.com");
        assertThat(found.displayName()).isEqualTo("Ada");

        found.updateProfile("ada.new@example.com", "Ada Lovelace", now.plusSeconds(30));
        accountRepository.save(found);

        assertThat(accountJpaRepository.count()).isEqualTo(1);
        Account updated = accountRepository.findByGoogleSubject("google-sub-1").orElseThrow();
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.email()).isEqualTo("ada.new@example.com");
        assertThat(updated.displayName()).isEqualTo("Ada Lovelace");
    }
}
