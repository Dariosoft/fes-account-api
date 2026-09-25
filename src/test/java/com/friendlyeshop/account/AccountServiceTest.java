package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    @Test
    void createsAccountWhenEmailIsNew() {
        when(accountRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.findOrCreate("New@Example.com", "Ada Lovelace");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(saved.getId()).isNotNull();
        assertThat(created.getId()).isEqualTo(saved.getId());
    }

    @Test
    void reusesExistingAccountForSameEmail() {
        Account existing = new Account("same@example.com", "Ada");
        UUID existingId = existing.getId();
        when(accountRepository.findByEmail("same@example.com")).thenReturn(Optional.of(existing));

        Account first = accountService.findOrCreate("same@example.com", "Ada");
        Account second = accountService.findOrCreate("SAME@example.com", "Ada");

        assertThat(first.getId()).isEqualTo(existingId);
        assertThat(second.getId()).isEqualTo(existingId);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
