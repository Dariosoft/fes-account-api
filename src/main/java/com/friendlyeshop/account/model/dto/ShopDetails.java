package com.friendlyeshop.account.model.dto;

public record ShopDetails(String name, String description) {
    public static ShopDetails from(ShopRequest request) {
        return new ShopDetails(request.name(), request.description());
    }
}
