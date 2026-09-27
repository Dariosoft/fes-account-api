package com.friendlyeshop.account.session.application;

import com.friendlyeshop.account.account.application.AccountRepository;
import com.friendlyeshop.account.account.domain.Account;
import com.friendlyeshop.account.session.domain.Session;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSession {
    private final SessionRepository sessionRepository;

    private final AccountRepository accountRepository;

    public GetSession(SessionRepository sessionRepository, AccountRepository accountRepository) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public SessionView execute(UUID sessionId) {
        if (sessionId == null) {
            return SessionView.unauthenticated();
        }
        Optional<Session> session = sessionRepository.findValidById(sessionId);
        if (session.isEmpty()) {
            return SessionView.unauthenticated();
        }
        Optional<Account> account = accountRepository.findById(session.get().accountId());
        if (account.isEmpty()) {
            return SessionView.unauthenticated();
        }
        Account found = account.get();
        return SessionView.authenticated(found.id(), found.email(), found.displayName());
    }
}
