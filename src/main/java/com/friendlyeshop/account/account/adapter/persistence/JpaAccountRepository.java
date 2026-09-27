package com.friendlyeshop.account.account.adapter.persistence;

import com.friendlyeshop.account.account.application.AccountRepository;
import com.friendlyeshop.account.account.domain.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaAccountRepository implements AccountRepository {
    static final String DEFAULT_ROLE = "USER";

    private final AccountJpaRepository jpaRepository;

    public JpaAccountRepository(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findByGoogleSubject(String googleSubject) {
        return jpaRepository.findByGoogleSub(googleSubject).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(UUID accountId) {
        return jpaRepository.findById(accountId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Account save(Account account) {
        AccountJpaEntity entity = jpaRepository.findById(account.id())
                .orElseGet(() -> newAccountEntity(account));
        entity.setEmail(account.email());
        entity.setDisplayName(account.displayName());
        entity.setUpdatedAt(account.updatedAt());
        return toDomain(jpaRepository.save(entity));
    }

    private AccountJpaEntity newAccountEntity(Account account) {
        return new AccountJpaEntity(
                account.id(),
                account.googleSubject(),
                account.email(),
                account.displayName(),
                null,
                DEFAULT_ROLE,
                account.createdAt(),
                account.updatedAt());
    }

    private Account toDomain(AccountJpaEntity entity) {
        return Account.reconstitute(
                entity.getId(),
                entity.getGoogleSub(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
