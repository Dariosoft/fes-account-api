package com.friendlyeshop.account.repository;

import com.friendlyeshop.account.model.Shop;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    Optional<Shop> findByAccountId(UUID accountId);
}
