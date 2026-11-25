package com.friendlyeshop.account.model.dto.out;

import com.friendlyeshop.account.model.Shop;

public record ShopResponse(String name, String description) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(shop.getName(), shop.getDescription());
    }
}
