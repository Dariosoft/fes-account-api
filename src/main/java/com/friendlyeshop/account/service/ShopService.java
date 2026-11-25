package com.friendlyeshop.account.service;

import com.friendlyeshop.account.exception.NotAuthenticatedException;
import com.friendlyeshop.account.exception.ShopNotFoundException;
import com.friendlyeshop.account.model.Shop;
import com.friendlyeshop.account.model.dto.out.SessionResponse;
import com.friendlyeshop.account.model.dto.in.ShopDetails;
import com.friendlyeshop.account.model.dto.out.ShopResponse;
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
        return ShopResponse.from(shop);
    }

    @Transactional
    public ShopResponse saveOwnShop(UUID sessionId, ShopDetails details) {
        UUID accountId = requireAccountId(sessionId);
        Instant now = Instant.now();
        Shop shop = shopRepository.findByAccountId(accountId)
                .map(existing -> {
                    existing.updateDetails(details, now);
                    return existing;
                })
                .orElseGet(() -> Shop.open(accountId, details, now));
        return ShopResponse.from(shopRepository.save(shop));
    }

    @Transactional(readOnly = true)
    public ShopResponse readPublicShop(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw ShopNotFoundException.accountMissing();
        }
        Shop shop = shopRepository.findByAccountId(accountId)
                .orElseThrow(ShopNotFoundException::publicShopMissing);
        return ShopResponse.from(shop);
    }

    private UUID requireAccountId(UUID sessionId) {
        SessionResponse session = sessionService.read(sessionId);
        if (!session.authenticated() || session.id() == null) {
            throw new NotAuthenticatedException();
        }
        return session.id();
    }
}
