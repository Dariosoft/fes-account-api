package com.friendlyeshop.account.session.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Logout {
    private final SessionRepository sessionRepository;

    public Logout(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public LogoutResult execute(UUID sessionId) {
        if (sessionId != null) {
            sessionRepository.invalidate(sessionId);
        }
        return LogoutResult.cookieCleared();
    }

    public record LogoutResult(boolean shouldClearCookie) {
        public static LogoutResult cookieCleared() {
            return new LogoutResult(true);
        }
    }
}
