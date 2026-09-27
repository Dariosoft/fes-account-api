package com.friendlyeshop.account.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    void createAssignsIdentityByGoogleSubject() {
        Account account = Account.create("google-sub-1", "a@example.com", "Ada", NOW);

        assertThat(account.id()).isNotNull();
        assertThat(account.googleSubject()).isEqualTo("google-sub-1");
        assertThat(account.email()).isEqualTo("a@example.com");
        assertThat(account.displayName()).isEqualTo("Ada");
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
        assertThat(account.belongsToGoogleSubject("google-sub-1")).isTrue();
        assertThat(account.belongsToGoogleSubject("other")).isFalse();
    }

    @Test
    void oneAccountPerGoogleSubjectHasNoStoreOrPanelVariant() {
        Account account = Account.create("same-sub", "shared@example.com", "Shared", NOW);

        assertThat(account.googleSubject()).isEqualTo("same-sub");
        assertThat(account.id()).isInstanceOf(UUID.class);
        // Domain models a single shared identity; there is no store/panel split field.
        assertThat(account).hasOnlyFields(
                "id", "googleSubject", "email", "displayName", "createdAt", "updatedAt");
    }

    @Test
    void updateProfileChangesEmailAndName() {
        Account account = Account.create("sub", "old@example.com", "Old", NOW);

        account.updateProfile("new@example.com", "New", NOW.plusSeconds(60));

        assertThat(account.email()).isEqualTo("new@example.com");
        assertThat(account.displayName()).isEqualTo("New");
        assertThat(account.updatedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(account.googleSubject()).isEqualTo("sub");
    }

    @Test
    void rejectsBlankGoogleSubject() {
        assertThatThrownBy(() -> Account.create(" ", "a@example.com", "Ada", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
