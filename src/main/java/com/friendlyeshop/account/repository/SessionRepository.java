package com.friendlyeshop.account.repository;

import com.friendlyeshop.account.model.Session;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {
}
