package com.friendlyeshop.account.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.account.domain.Account;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountRepositoryPortTest {
    @Test
    void stubCanSaveAndFindByGoogleSubject() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        Account account = Account.create("sub-1", "a@example.com", "Ada", Instant.parse("2026-09-26T12:00:00Z"));

        repository.save(account);

        assertThat(repository.findByGoogleSubject("sub-1")).isPresent().get()
                .extracting(Account::googleSubject, Account::email)
                .containsExactly("sub-1", "a@example.com");
        assertThat(repository.findByGoogleSubject("missing")).isEmpty();
    }

    private static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> bySubject = new HashMap<>();

        @Override
        public Optional<Account> findByGoogleSubject(String googleSubject) {
            return Optional.ofNullable(bySubject.get(googleSubject));
        }

        @Override
        public Optional<Account> findById(java.util.UUID accountId) {
            return bySubject.values().stream().filter(account -> account.id().equals(accountId)).findFirst();
        }

        @Override
        public Account save(Account account) {
            bySubject.put(account.googleSubject(), account);
            return account;
        }
    }
}
