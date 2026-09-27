package com.friendlyeshop.account.service;

import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.dto.SessionResponse;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.SessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;

    private final AccountRepository accountRepository;

    public SessionService(SessionRepository sessionRepository, AccountRepository accountRepository) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public SessionResponse read(UUID sessionId) {
        if (sessionId == null) {
            return SessionResponse.anonymous();
        }
        return sessionRepository.findById(sessionId)
                .filter(session -> session.isValid(Instant.now()))
                .flatMap(session -> accountRepository.findById(session.getAccountId()))
                .map(this::authenticated)
                .orElseGet(SessionResponse::anonymous);
    }

    @Transactional
    public void logout(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.revoke(Instant.now());
            sessionRepository.save(session);
        });
    }

    private SessionResponse authenticated(Account account) {
        return SessionResponse.authenticated(account.getId(), account.getEmail(), account.getDisplayName());
    }
}
