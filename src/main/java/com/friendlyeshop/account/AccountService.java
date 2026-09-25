package com.friendlyeshop.account;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account findOrCreate(String email, String displayName) {
        String normalizedEmail = Account.normalizeEmail(email);
        return accountRepository.findByEmail(normalizedEmail)
                .map(existing -> refreshDisplayName(existing, displayName))
                .orElseGet(() -> accountRepository.save(new Account(normalizedEmail, displayName)));
    }

    @Transactional(readOnly = true)
    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(Account.normalizeEmail(email));
    }

    private Account refreshDisplayName(Account account, String displayName) {
        if (displayName != null && !displayName.equals(account.getDisplayName())) {
            account.setDisplayName(displayName);
            return accountRepository.save(account);
        }
        return account;
    }
}
