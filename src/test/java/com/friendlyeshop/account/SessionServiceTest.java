package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository);
    }

    @Test
    void createThenResolveReturnsAccount() {
        Account account = new Account("user@example.com", "User");
        Session session = new Session(account);
        when(sessionRepository.save(any(Session.class))).thenReturn(session);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        Session created = sessionService.create(account);
        Optional<Session> resolved = sessionService.resolve(created.getId());

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getAccount().getId()).isEqualTo(account.getId());
        verify(sessionRepository).deleteByAccountId(account.getId());
    }

    @Test
    void invalidateMakesResolveReturnEmpty() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        sessionService.invalidate(sessionId);
        Optional<Session> resolved = sessionService.resolve(sessionId);

        assertThat(resolved).isEmpty();
        verify(sessionRepository).deleteById(sessionId);
    }
}
