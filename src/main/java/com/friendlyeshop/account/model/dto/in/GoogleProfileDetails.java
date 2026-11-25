package com.friendlyeshop.account.model.dto.in;

public record GoogleProfileDetails(String subject, String email, String name) {
    public static GoogleProfileDetails from(String subject, String email, String name) {
        return new GoogleProfileDetails(subject, email, name);
    }
}
