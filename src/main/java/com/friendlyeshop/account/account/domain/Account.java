package com.friendlyeshop.account.account.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Identity owned by account-api. One account per Google subject; no parallel store/panel account.
 */
public final class Account {
    private final UUID id;

    private final String googleSubject;

    private String email;

    private String displayName;

    private final Instant createdAt;

    private Instant updatedAt;

    private Account(
            UUID id,
            String googleSubject,
            String email,
            String displayName,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.googleSubject = requireText(googleSubject, "googleSubject");
        this.email = requireText(email, "email");
        this.displayName = requireText(displayName, "displayName");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Account create(String googleSubject, String email, String displayName, Instant now) {
        Instant createdAt = Objects.requireNonNull(now, "now");
        return new Account(UUID.randomUUID(), googleSubject, email, displayName, createdAt, createdAt);
    }

    public static Account reconstitute(
            UUID id,
            String googleSubject,
            String email,
            String displayName,
            Instant createdAt,
            Instant updatedAt) {
        return new Account(id, googleSubject, email, displayName, createdAt, updatedAt);
    }

    public void updateProfile(String email, String displayName, Instant now) {
        this.email = requireText(email, "email");
        this.displayName = requireText(displayName, "displayName");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public UUID id() {
        return id;
    }

    public String googleSubject() {
        return googleSubject;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean belongsToGoogleSubject(String subject) {
        return googleSubject.equals(subject);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
