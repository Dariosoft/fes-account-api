package com.friendlyeshop.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public Session create(Account account) {
        sessionRepository.deleteByAccountId(account.getId());
        return sessionRepository.save(new Session(account));
    }

    @Transactional(readOnly = true)
    public Optional<Session> resolve(UUID sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        return sessionRepository.findById(sessionId);
    }

    @Transactional
    public void invalidate(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionRepository.deleteById(sessionId);
    }
}
