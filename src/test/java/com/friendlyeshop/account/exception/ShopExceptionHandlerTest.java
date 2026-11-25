package com.friendlyeshop.account.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.model.dto.ShopRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ShopExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsNotAuthenticatedTo401() throws Exception {
        mockMvc.perform(get("/probe/not-authenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Necesitás iniciar sesión para administrar tu tienda."));
    }

    @Test
    void mapsMissingOwnShopTo404() throws Exception {
        mockMvc.perform(get("/probe/own-shop-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SHOP_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todavía no configuraste tu tienda."));
    }

    @Test
    void mapsMissingPublicShopTo404() throws Exception {
        mockMvc.perform(get("/probe/public-shop-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SHOP_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ese vendedor todavía no tiene tienda."));
    }

    @Test
    void mapsMissingAccountTo404() throws Exception {
        mockMvc.perform(get("/probe/account-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("La cuenta indicada no existe."));
    }

    @Test
    void mapsInvalidBodyTo400WithSpanishMessage() throws Exception {
        mockMvc.perform(post("/probe/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El nombre de la tienda es obligatorio."));
    }

    @RestController
    public static class ProbeController {
        @GetMapping("/probe/not-authenticated")
        public void notAuthenticated() {
            throw new NotAuthenticatedException();
        }

        @GetMapping("/probe/own-shop-missing")
        public void ownShopMissing() {
            throw ShopNotFoundException.ownShopMissing();
        }

        @GetMapping("/probe/public-shop-missing")
        public void publicShopMissing() {
            throw ShopNotFoundException.publicShopMissing();
        }

        @GetMapping("/probe/account-missing")
        public void accountMissing() {
            throw ShopNotFoundException.accountMissing();
        }

        @PostMapping("/probe/validate")
        public void validate(@Valid @RequestBody ShopRequest request) {
        }
    }
}
