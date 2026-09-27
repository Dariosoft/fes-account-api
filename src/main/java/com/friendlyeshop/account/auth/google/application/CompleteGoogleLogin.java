package com.friendlyeshop.account.auth.google.application;

import com.friendlyeshop.account.account.application.AccountRepository;
import com.friendlyeshop.account.account.domain.Account;
import com.friendlyeshop.account.session.application.SessionRepository;
import com.friendlyeshop.account.session.domain.Session;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CompleteGoogleLogin {
    private final GoogleAuthClient googleAuthClient;

    private final AccountRepository accountRepository;

    private final SessionRepository sessionRepository;

    private final Clock clock;

    public CompleteGoogleLogin(
            GoogleAuthClient googleAuthClient,
            AccountRepository accountRepository,
            SessionRepository sessionRepository,
            Clock clock) {
        this.googleAuthClient = googleAuthClient;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Transactional
    public Result execute(String code, String state) {
        Optional<String> returnTo = StartGoogleLogin.decodeReturnTo(state);
        if (returnTo.isEmpty()) {
            return Result.failed("http://localhost");
        }
        String destination = returnTo.get();
        Optional<GoogleAuthClient.GoogleIdentity> identity = googleAuthClient.exchangeCode(code);
        if (identity.isEmpty()) {
            return Result.failed(withLoginError(destination));
        }
        Account account = upsertAccount(identity.get());
        Session session = Session.open(account.id(), Instant.now(clock));
        sessionRepository.save(session);
        return Result.succeeded(destination, session.id());
    }

    private Account upsertAccount(GoogleAuthClient.GoogleIdentity identity) {
        Instant now = Instant.now(clock);
        Optional<Account> existing = accountRepository.findByGoogleSubject(identity.subject());
        if (existing.isPresent()) {
            Account account = existing.get();
            account.updateProfile(identity.email(), identity.name(), now);
            return accountRepository.save(account);
        }
        Account created = Account.create(identity.subject(), identity.email(), identity.name(), now);
        return accountRepository.save(created);
    }

    private static String withLoginError(String returnTo) {
        return UriComponentsBuilder.fromUriString(returnTo)
                .queryParam("login_error", "1")
                .build(true)
                .toUriString();
    }

    public record Result(boolean success, String redirectUrl, UUID sessionId) {
        public static Result succeeded(String redirectUrl, UUID sessionId) {
            return new Result(true, redirectUrl, sessionId);
        }

        public static Result failed(String redirectUrl) {
            return new Result(false, redirectUrl, null);
        }
    }
}
