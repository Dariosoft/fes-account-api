package com.friendlyeshop.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    static final String DEFAULT_ROLE = "USER";

    @Id
    private UUID id;

    @Column(name = "google_sub", nullable = false, unique = true, length = 255)
    private String googleSub;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 320)
    private String displayName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public static Account open(String googleSub, String email, String displayName, Instant now) {
        Instant createdAt = Objects.requireNonNull(now, "now");
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.googleSub = requireText(googleSub, "googleSub");
        account.email = requireText(email, "email");
        account.displayName = requireText(displayName, "displayName");
        account.role = DEFAULT_ROLE;
        account.createdAt = createdAt;
        account.updatedAt = createdAt;
        return account;
    }

    public void updateProfile(String email, String displayName, Instant now) {
        this.email = requireText(email, "email");
        this.displayName = requireText(displayName, "displayName");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public UUID getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
