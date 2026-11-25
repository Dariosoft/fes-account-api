package com.friendlyeshop.account.controller;

import com.friendlyeshop.account.model.dto.ShopResponse;
import com.friendlyeshop.account.service.ShopService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shops")
public class PublicShopController {
    private final ShopService shopService;

    public PublicShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/{accountId}")
    public ShopResponse publicShop(@PathVariable UUID accountId) {
        return shopService.readPublicShop(accountId);
    }
}
