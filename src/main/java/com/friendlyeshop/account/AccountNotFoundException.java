package com.friendlyeshop.account;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String email) {
        super("No se encontró una cuenta para el correo indicado.");
    }
}
