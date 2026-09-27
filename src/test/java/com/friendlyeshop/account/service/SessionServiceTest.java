package com.friendlyeshop.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.Session;
import com.friendlyeshop.account.model.dto.SessionResponse;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.SessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void returnsAnonymousWhenCookieAbsentOrUnknown() {
        when(sessionRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(sessionService.read(null).authenticated()).isFalse();
        assertThat(sessionService.read(UUID.randomUUID()).authenticated()).isFalse();
    }

    @Test
    void returnsAccountWhenSessionIsValid() {
        Account account = Account.open("sub", "ada@example.com", "Ada", NOW);
        Session session = Session.open(account.getId(), NOW);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        SessionResponse response = sessionService.read(session.getId());

        assertThat(response.authenticated()).isTrue();
        assertThat(response.id()).isEqualTo(account.getId());
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.name()).isEqualTo("Ada");
    }

    @Test
    void logoutRevokesTheSession() {
        Session session = Session.open(UUID.randomUUID(), NOW);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        sessionService.logout(session.getId());

        assertThat(session.isValid(NOW.plusSeconds(1))).isFalse();
        verify(sessionRepository).save(session);
    }

    @Test
    void logoutWithoutSessionDoesNotTouchTheRepository() {
        sessionService.logout(null);

        verify(sessionRepository, never()).findById(any());
    }
}
