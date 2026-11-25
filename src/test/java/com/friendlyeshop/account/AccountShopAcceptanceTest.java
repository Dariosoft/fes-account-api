package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.friendlyeshop.account.http.cookie.SessionCookieWriter;
import com.friendlyeshop.account.model.Account;
import com.friendlyeshop.account.model.Session;
import com.friendlyeshop.account.model.Shop;
import com.friendlyeshop.account.model.dto.ShopDetails;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.SessionRepository;
import com.friendlyeshop.account.repository.ShopRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AccountShopAcceptanceTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ShopRepository shopRepository;

    @BeforeEach
    void cleanDatabase() {
        shopRepository.deleteAll();
        sessionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void authenticatedSellerUpsertsOwnShopAndAnonymousVisitorReadsPublicData() throws Exception {
        Account seller = accountRepository.save(
                Account.open("seller-sub", "seller@example.com", "Seller", Instant.now()));
        UUID sellerSessionId = sessionRepository.save(Session.open(seller.getId(), Instant.now())).getId();
        Cookie sellerCookie = sessionCookie(sellerSessionId);

        mockMvc.perform(put("/accounts/shop")
                        .cookie(sellerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tienda de Ada\",\"description\":\"Cosas bonitas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda de Ada"))
                .andExpect(jsonPath("$.description").value("Cosas bonitas"));

        mockMvc.perform(put("/accounts/shop")
                        .cookie(sellerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tienda nueva\",\"description\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda nueva"))
                .andExpect(jsonPath("$.description").value(nullValue()));

        assertThat(shopRepository.count()).isEqualTo(1);
        Shop shop = shopRepository.findByAccountId(seller.getId()).orElseThrow();
        assertThat(shop.getName()).isEqualTo("Tienda nueva");
        assertThat(shop.getDescription()).isNull();

        MvcResult publicResult = mockMvc.perform(get("/accounts/shops/{accountId}", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda nueva"))
                .andReturn();

        assertThat(publicResult.getResponse().getContentAsString()).doesNotContain("email");

        Account stranger = accountRepository.save(
                Account.open("stranger-sub", "stranger@example.com", "Stranger", Instant.now()));
        mockMvc.perform(get("/accounts/shops/{accountId}", stranger.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SHOP_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ese vendedor todavía no tiene tienda."));

        mockMvc.perform(get("/accounts/shops/{accountId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("La cuenta indicada no existe."));

        mockMvc.perform(put("/accounts/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Intruso\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH_REQUIRED"));

        assertThat(shopRepository.count()).isEqualTo(1);
    }

    @Test
    void anotherAccountCannotModifyTheSellersShop() throws Exception {
        Account seller = accountRepository.save(
                Account.open("owner-sub", "owner@example.com", "Owner", Instant.now()));
        UUID sellerSessionId = sessionRepository.save(Session.open(seller.getId(), Instant.now())).getId();
        shopRepository.save(Shop.open(
                seller.getId(), new ShopDetails("Tienda del dueño", "Original"), Instant.now()));

        Account other = accountRepository.save(
                Account.open("other-sub", "other@example.com", "Other", Instant.now()));
        UUID otherSessionId = sessionRepository.save(Session.open(other.getId(), Instant.now())).getId();

        mockMvc.perform(put("/accounts/shop")
                        .cookie(sessionCookie(otherSessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tienda ajena\",\"description\":\"Intento\"}"))
                .andExpect(status().isOk());

        Shop sellerShop = shopRepository.findByAccountId(seller.getId()).orElseThrow();
        assertThat(sellerShop.getName()).isEqualTo("Tienda del dueño");
        assertThat(sellerShop.getDescription()).isEqualTo("Original");
        assertThat(shopRepository.count()).isEqualTo(2);
    }

    private static Cookie sessionCookie(UUID sessionId) {
        return new Cookie(SessionCookieWriter.COOKIE_NAME, sessionId.toString());
    }
}
