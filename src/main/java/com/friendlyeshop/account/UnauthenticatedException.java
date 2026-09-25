package com.friendlyeshop.account;

public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("No hay una persona autenticada.");
    }
}
