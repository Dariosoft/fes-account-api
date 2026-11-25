package com.friendlyeshop.account.exception;

public class ShopNotFoundException extends RuntimeException {
    public static final String SHOP_NOT_FOUND = "SHOP_NOT_FOUND";

    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";

    private final String error;

    private ShopNotFoundException(String error, String message) {
        super(message);
        this.error = error;
    }

    public static ShopNotFoundException ownShopMissing() {
        return new ShopNotFoundException(SHOP_NOT_FOUND, "Todavía no configuraste tu tienda.");
    }

    public static ShopNotFoundException publicShopMissing() {
        return new ShopNotFoundException(SHOP_NOT_FOUND, "Ese vendedor todavía no tiene tienda.");
    }

    public static ShopNotFoundException accountMissing() {
        return new ShopNotFoundException(ACCOUNT_NOT_FOUND, "La cuenta indicada no existe.");
    }

    public String getError() {
        return error;
    }
}
