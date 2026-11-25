package com.friendlyeshop.account.controller;

import com.friendlyeshop.account.http.cookie.SessionCookieWriter;
import com.friendlyeshop.account.model.dto.ShopRequest;
import com.friendlyeshop.account.model.dto.ShopResponse;
import com.friendlyeshop.account.service.ShopService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShopController {
    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/shop")
    public ShopResponse ownShop(
            @CookieValue(value = SessionCookieWriter.COOKIE_NAME, required = false) UUID sessionId) {
        return shopService.readOwnShop(sessionId);
    }

    @PutMapping("/shop")
    public ShopResponse saveOwnShop(
            @CookieValue(value = SessionCookieWriter.COOKIE_NAME, required = false) UUID sessionId,
            @Valid @RequestBody ShopRequest request) {
        return shopService.saveOwnShop(sessionId, request.name(), request.description());
    }

    @GetMapping("/shops/{accountId}")
    public ShopResponse publicShop(@PathVariable UUID accountId) {
        return shopService.readPublicShop(accountId);
    }
}
