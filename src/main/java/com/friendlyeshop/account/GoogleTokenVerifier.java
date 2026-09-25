package com.friendlyeshop.account;

@FunctionalInterface
public interface GoogleTokenVerifier {

    GoogleIdentity verify(String idToken);
}
