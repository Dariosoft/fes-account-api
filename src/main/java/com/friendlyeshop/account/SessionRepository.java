package com.friendlyeshop.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    void deleteByAccountId(UUID accountId);
}
