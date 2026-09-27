package com.friendlyeshop.account.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AccountTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    void openAssignsOneIdentityForTheGoogleSubject() {
        Account account = Account.open("google-sub-1", "a@example.com", "Ada", NOW);

        assertThat(account.getId()).isNotNull();
        assertThat(account.getGoogleSub()).isEqualTo("google-sub-1");
        assertThat(account.getEmail()).isEqualTo("a@example.com");
        assertThat(account.getDisplayName()).isEqualTo("Ada");
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void updateProfileChangesEmailAndNameAndKeepsTheGoogleSubject() {
        Account account = Account.open("sub", "old@example.com", "Old", NOW);

        account.updateProfile("new@example.com", "New", NOW.plusSeconds(60));

        assertThat(account.getEmail()).isEqualTo("new@example.com");
        assertThat(account.getDisplayName()).isEqualTo("New");
        assertThat(account.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(account.getGoogleSub()).isEqualTo("sub");
    }

    @Test
    void rejectsBlankGoogleSubject() {
        assertThatThrownBy(() -> Account.open(" ", "a@example.com", "Ada", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
