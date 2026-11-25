package com.friendlyeshop.account.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.Shop;
import com.friendlyeshop.account.model.dto.ShopDetails;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ShopRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void savesAndFindsShopByAccountId() {
        UUID accountId = seedAccount("shop-sub-1", "shop1@example.com");

        Shop saved = shopRepository.saveAndFlush(
                Shop.open(accountId, new ShopDetails("Tienda", "Descripción"), NOW));

        Shop found = shopRepository.findByAccountId(accountId).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getAccountId()).isEqualTo(accountId);
        assertThat(found.getName()).isEqualTo("Tienda");
        assertThat(found.getDescription()).isEqualTo("Descripción");
    }

    @Test
    void rejectsASecondShopForTheSameAccount() {
        UUID accountId = seedAccount("shop-sub-2", "shop2@example.com");
        shopRepository.saveAndFlush(Shop.open(accountId, new ShopDetails("Tienda", null), NOW));

        assertThatThrownBy(() -> shopRepository.saveAndFlush(
                        Shop.open(accountId, new ShopDetails("Otra", null), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsTwoAccountsToShareTheSameShopName() {
        UUID firstAccount = seedAccount("shop-sub-3", "shop3@example.com");
        UUID secondAccount = seedAccount("shop-sub-4", "shop4@example.com");

        shopRepository.saveAndFlush(Shop.open(firstAccount, new ShopDetails("Misma tienda", null), NOW));
        shopRepository.saveAndFlush(Shop.open(secondAccount, new ShopDetails("Misma tienda", null), NOW));

        assertThat(shopRepository.findByAccountId(firstAccount)).isPresent();
        assertThat(shopRepository.findByAccountId(secondAccount)).isPresent();
    }

    private UUID seedAccount(String googleSub, String email) {
        Account account = accountRepository.saveAndFlush(Account.open(googleSub, email, "Tienda User", NOW));
        return account.getId();
    }
}
