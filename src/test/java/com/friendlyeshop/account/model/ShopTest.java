package com.friendlyeshop.account.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShopTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void openTrimsNameAndDescription() {
        Shop shop = Shop.open(ACCOUNT_ID, "  Tienda de Ada  ", "  Cosas bonitas  ", NOW);

        assertThat(shop.getName()).isEqualTo("Tienda de Ada");
        assertThat(shop.getDescription()).isEqualTo("Cosas bonitas");
        assertThat(shop.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(shop.getCreatedAt()).isEqualTo(NOW);
        assertThat(shop.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void acceptsNameWithOneAndEightyCharacters() {
        Shop single = Shop.open(ACCOUNT_ID, "a", null, NOW);
        Shop max = Shop.open(ACCOUNT_ID, "a".repeat(Shop.NAME_MAX_LENGTH), null, NOW);

        assertThat(single.getName()).hasSize(1);
        assertThat(max.getName()).hasSize(Shop.NAME_MAX_LENGTH);
    }

    @Test
    void rejectsMissingBlankOrTooLongName() {
        assertThatThrownBy(() -> Shop.open(ACCOUNT_ID, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Shop.open(ACCOUNT_ID, "   ", null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Shop.open(ACCOUNT_ID, "a".repeat(Shop.NAME_MAX_LENGTH + 1), null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsMissingBlankDescriptionToNull() {
        assertThat(Shop.open(ACCOUNT_ID, "Tienda", null, NOW).getDescription()).isNull();
        assertThat(Shop.open(ACCOUNT_ID, "Tienda", "   ", NOW).getDescription()).isNull();
    }

    @Test
    void acceptsDescriptionWithMaximumLengthAndRejectsLonger() {
        Shop max = Shop.open(ACCOUNT_ID, "Tienda", "d".repeat(Shop.DESCRIPTION_MAX_LENGTH), NOW);

        assertThat(max.getDescription()).hasSize(Shop.DESCRIPTION_MAX_LENGTH);
        assertThatThrownBy(() -> Shop.open(ACCOUNT_ID, "Tienda",
                "d".repeat(Shop.DESCRIPTION_MAX_LENGTH + 1), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateDetailsReplacesDataAndTimestamp() {
        Shop shop = Shop.open(ACCOUNT_ID, "Tienda", "Vieja", NOW);

        shop.updateDetails("  Nueva tienda  ", "  Nueva descripción  ", NOW.plusSeconds(60));

        assertThat(shop.getName()).isEqualTo("Nueva tienda");
        assertThat(shop.getDescription()).isEqualTo("Nueva descripción");
        assertThat(shop.getCreatedAt()).isEqualTo(NOW);
        assertThat(shop.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }
}
