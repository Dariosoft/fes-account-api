package com.friendlyeshop.account.service;

import com.friendlyeshop.account.exception.NotAuthenticatedException;
import com.friendlyeshop.account.exception.ShopNotFoundException;
import com.friendlyeshop.account.model.Shop;
import com.friendlyeshop.account.model.dto.SessionResponse;
import com.friendlyeshop.account.model.dto.ShopResponse;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.ShopRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {
    private final ShopRepository shopRepository;

    private final AccountRepository accountRepository;

    private final SessionService sessionService;

    public ShopService(
            ShopRepository shopRepository,
            AccountRepository accountRepository,
            SessionService sessionService) {
        this.shopRepository = shopRepository;
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public ShopResponse readOwnShop(UUID sessionId) {
        UUID accountId = requireAccountId(sessionId);
        Shop shop = shopRepository.findByAccountId(accountId)
                .orElseThrow(ShopNotFoundException::ownShopMissing);
        return toResponse(shop);
    }

    @Transactional
    public ShopResponse saveOwnShop(UUID sessionId, String name, String description) {
        UUID accountId = requireAccountId(sessionId);
        Instant now = Instant.now();
        Shop shop = shopRepository.findByAccountId(accountId)
                .map(existing -> updateDetails(existing, name, description, now))
                .orElseGet(() -> Shop.open(accountId, name, description, now));
        return toResponse(shopRepository.save(shop));
    }

    @Transactional(readOnly = true)
    public ShopResponse readPublicShop(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw ShopNotFoundException.accountMissing();
        }
        Shop shop = shopRepository.findByAccountId(accountId)
                .orElseThrow(ShopNotFoundException::publicShopMissing);
        return toResponse(shop);
    }

    private UUID requireAccountId(UUID sessionId) {
        SessionResponse session = sessionService.read(sessionId);
        if (!session.authenticated() || session.id() == null) {
            throw new NotAuthenticatedException();
        }
        return session.id();
    }

    private static Shop updateDetails(Shop shop, String name, String description, Instant now) {
        shop.updateDetails(name, description, now);
        return shop;
    }

    private static ShopResponse toResponse(Shop shop) {
        return new ShopResponse(shop.getName(), shop.getDescription());
    }
}
