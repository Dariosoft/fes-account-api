package com.friendlyeshop.account.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.config.AuthConfig;
import com.friendlyeshop.account.exception.NotAuthenticatedException;
import com.friendlyeshop.account.exception.ShopNotFoundException;
import com.friendlyeshop.account.http.cookie.SessionCookieWriter;
import com.friendlyeshop.account.model.dto.ShopResponse;
import com.friendlyeshop.account.service.ShopService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShopController.class)
@Import(AuthConfig.class)
class ShopControllerTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopService shopService;

    @Test
    void ownShopReturnsDataWhenAuthenticated() throws Exception {
        when(shopService.readOwnShop(SESSION_ID)).thenReturn(new ShopResponse("Tienda de Ada", null));

        mockMvc.perform(get("/accounts/shop").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda de Ada"))
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void ownShopRequiresSession() throws Exception {
        when(shopService.readOwnShop(null)).thenThrow(new NotAuthenticatedException());

        mockMvc.perform(get("/accounts/shop"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH_REQUIRED"));
    }

    @Test
    void savingShopDelegatesTrimmedDataToService() throws Exception {
        when(shopService.saveOwnShop(SESSION_ID, "Tienda de Ada", "Cosas bonitas"))
                .thenReturn(new ShopResponse("Tienda de Ada", "Cosas bonitas"));

        mockMvc.perform(put("/accounts/shop")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Tienda de Ada  \",\"description\":\"  Cosas bonitas  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda de Ada"))
                .andExpect(jsonPath("$.description").value("Cosas bonitas"));

        verify(shopService).saveOwnShop(SESSION_ID, "Tienda de Ada", "Cosas bonitas");
    }

    @Test
    void savingShopRequiresSession() throws Exception {
        when(shopService.saveOwnShop(null, "Tienda", null)).thenThrow(new NotAuthenticatedException());

        mockMvc.perform(put("/accounts/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tienda\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH_REQUIRED"));
    }

    @Test
    void publicShopReturnsDataWithoutSession() throws Exception {
        when(shopService.readPublicShop(ACCOUNT_ID)).thenReturn(new ShopResponse("Tienda de Ada", "Cosas"));

        mockMvc.perform(get("/accounts/shops/{accountId}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda de Ada"))
                .andExpect(jsonPath("$.description").value("Cosas"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void publicShopAllowsStorefrontOriginWithCredentials() throws Exception {
        when(shopService.readPublicShop(ACCOUNT_ID)).thenReturn(new ShopResponse("Tienda de Ada", null));

        mockMvc.perform(get("/accounts/shops/{accountId}", ACCOUNT_ID)
                        .header(HttpHeaders.ORIGIN, "http://store.example.test"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://store.example.test"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void publicShopReportsVendorWithoutShop() throws Exception {
        when(shopService.readPublicShop(ACCOUNT_ID)).thenThrow(ShopNotFoundException.publicShopMissing());

        mockMvc.perform(get("/accounts/shops/{accountId}", ACCOUNT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SHOP_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ese vendedor todavía no tiene tienda."));
    }

    @Test
    void publicShopReportsUnknownAccount() throws Exception {
        when(shopService.readPublicShop(ACCOUNT_ID)).thenThrow(ShopNotFoundException.accountMissing());

        mockMvc.perform(get("/accounts/shops/{accountId}", ACCOUNT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("La cuenta indicada no existe."));
    }

    @Test
    void rejectsMissingNameWithoutSaving() throws Exception {
        assertValidationError("{\"description\":\"Cosas\"}", "El nombre de la tienda es obligatorio.");
        verifyNoInteractions(shopService);
    }

    @Test
    void rejectsBlankNameWithoutSaving() throws Exception {
        assertValidationError("{\"name\":\"   \"}", "El nombre de la tienda es obligatorio.");
        verifyNoInteractions(shopService);
    }

    @Test
    void rejectsTooLongNameWithoutSaving() throws Exception {
        assertValidationError(
                "{\"name\":\"" + "a".repeat(81) + "\"}",
                "El nombre de la tienda no puede superar los 80 caracteres.");
        verifyNoInteractions(shopService);
    }

    @Test
    void rejectsTooLongDescriptionWithoutSaving() throws Exception {
        assertValidationError(
                "{\"name\":\"Tienda\",\"description\":\"" + "d".repeat(1001) + "\"}",
                "La descripción no puede superar los 1000 caracteres.");
        verifyNoInteractions(shopService);
    }

    @Test
    void savesBlankDescriptionAsNull() throws Exception {
        when(shopService.saveOwnShop(SESSION_ID, "Tienda", null)).thenReturn(new ShopResponse("Tienda", null));

        mockMvc.perform(put("/accounts/shop")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tienda\",\"description\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(nullValue()));

        verify(shopService).saveOwnShop(SESSION_ID, "Tienda", null);
    }

    private void assertValidationError(String body, String expectedMessage) throws Exception {
        mockMvc.perform(put("/accounts/shop")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private static Cookie sessionCookie() {
        return new Cookie(SessionCookieWriter.COOKIE_NAME, SESSION_ID.toString());
    }
}
