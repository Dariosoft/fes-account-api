package com.friendlyeshop.account.model.dto;

import com.friendlyeshop.account.model.Shop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShopRequest(
        @NotBlank(message = "El nombre de la tienda es obligatorio.")
        @Size(max = Shop.NAME_MAX_LENGTH,
                message = "El nombre de la tienda no puede superar los " + Shop.NAME_MAX_LENGTH + " caracteres.")
        String name,
        @Size(max = Shop.DESCRIPTION_MAX_LENGTH,
                message = "La descripción no puede superar los " + Shop.DESCRIPTION_MAX_LENGTH + " caracteres.")
        String description) {
}
