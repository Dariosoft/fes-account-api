package com.friendlyeshop.account.exception;

public class NotAuthenticatedException extends RuntimeException {
    public static final String ERROR_CODE = "AUTH_REQUIRED";

    public NotAuthenticatedException() {
        super("Necesitás iniciar sesión para administrar tu tienda.");
    }
}
