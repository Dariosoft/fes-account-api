package com.friendlyeshop.account.repository;

import com.friendlyeshop.account.model.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByGoogleSub(String googleSub);
}
