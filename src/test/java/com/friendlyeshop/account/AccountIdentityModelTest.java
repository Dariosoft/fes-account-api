package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountIdentityModelTest {

    @Mock
    private AccountRepository accountRepository;

    @Test
    void accountIsIdentityOnlyWithoutPasswordOrRoles() {
        Set<String> fieldNames = Arrays.stream(Account.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fieldNames).contains("id", "email", "displayName", "createdAt", "updatedAt");
        assertThat(fieldNames).doesNotContain("passwordHash", "password", "role", "roles");
    }

    @Test
    void findOrCreateReusesSingleAccountPerEmail() {
        AccountService service = new AccountService(accountRepository);
        Account existing = new Account("owner@example.com", "Owner");
        when(accountRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account first = service.findOrCreate("Owner@Example.com", "Owner");
        Account second = service.findOrCreate("owner@example.com", "Owner");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(first.getId()).isEqualTo(captor.getValue().getId());
        assertThat(second.getId()).isEqualTo(existing.getId());
        verify(accountRepository, never()).delete(any());
    }
}
