package com.friendlyeshop.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "shops")
public class Shop {
    public static final int NAME_MAX_LENGTH = 80;

    public static final int DESCRIPTION_MAX_LENGTH = 1000;

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shop() {
    }

    public static Shop open(UUID accountId, String name, String description, Instant now) {
        Instant createdAt = Objects.requireNonNull(now, "now");
        Shop shop = new Shop();
        shop.id = UUID.randomUUID();
        shop.accountId = Objects.requireNonNull(accountId, "accountId");
        shop.name = normalizeName(name);
        shop.description = normalizeDescription(description);
        shop.createdAt = createdAt;
        shop.updatedAt = createdAt;
        return shop;
    }

    public void updateDetails(String name, String description, Instant now) {
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String normalizeName(String value) {
        String stripped = value == null ? "" : value.strip();
        if (stripped.isEmpty() || stripped.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "name must have between 1 and " + NAME_MAX_LENGTH + " characters");
        }
        return stripped;
    }

    private static String normalizeDescription(String value) {
        String stripped = value == null ? "" : value.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        if (stripped.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "description must not exceed " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        return stripped;
    }
}
