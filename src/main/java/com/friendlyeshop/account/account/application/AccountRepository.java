package com.friendlyeshop.account.account.application;

import com.friendlyeshop.account.account.domain.Account;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findByGoogleSubject(String googleSubject);

    Optional<Account> findById(UUID accountId);

    Account save(Account account);
}
