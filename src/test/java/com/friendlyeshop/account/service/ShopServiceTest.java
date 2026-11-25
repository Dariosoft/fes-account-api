package com.friendlyeshop.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.friendlyeshop.account.exception.NotAuthenticatedException;
import com.friendlyeshop.account.exception.ShopNotFoundException;
import com.friendlyeshop.account.model.Shop;
import com.friendlyeshop.account.model.dto.SessionResponse;
import com.friendlyeshop.account.model.dto.ShopResponse;
import com.friendlyeshop.account.repository.AccountRepository;
import com.friendlyeshop.account.repository.ShopRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID SESSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private ShopService shopService;

    @Test
    void rejectsReadWithoutSessionWithoutTouchingShopRepository() {
        when(sessionService.read(SESSION_ID)).thenReturn(SessionResponse.anonymous());

        assertThatThrownBy(() -> shopService.readOwnShop(SESSION_ID))
                .isInstanceOf(NotAuthenticatedException.class);

        verifyNoInteractions(shopRepository);
    }

    @Test
    void rejectsSaveWithoutSessionWithoutTouchingShopRepository() {
        when(sessionService.read(null)).thenReturn(SessionResponse.anonymous());

        assertThatThrownBy(() -> shopService.saveOwnShop(null, "Tienda", null))
                .isInstanceOf(NotAuthenticatedException.class);

        verifyNoInteractions(shopRepository);
    }

    @Test
    void firstSaveCreatesShopForAuthenticatedAccount() {
        authenticate(ACCOUNT_ID);
        when(shopRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopResponse response = shopService.saveOwnShop(SESSION_ID, "  Tienda de Ada  ", "  Cosas  ");

        Shop saved = captureSavedShop();
        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(saved.getName()).isEqualTo("Tienda de Ada");
        assertThat(saved.getDescription()).isEqualTo("Cosas");
        assertThat(response).isEqualTo(new ShopResponse("Tienda de Ada", "Cosas"));
    }

    @Test
    void secondSaveUpdatesTheSameShopWithoutCreatingAnother() {
        authenticate(ACCOUNT_ID);
        Shop existing = Shop.open(ACCOUNT_ID, "Vieja", "Vieja descripción", NOW);
        when(shopRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopResponse response = shopService.saveOwnShop(SESSION_ID, "Nueva", null);

        Shop saved = captureSavedShop();
        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getName()).isEqualTo("Nueva");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(NOW);
        assertThat(response).isEqualTo(new ShopResponse("Nueva", null));
    }

    @Test
    void savingOnlyTouchesTheAccountResolvedFromTheSession() {
        when(sessionService.read(SESSION_ID))
                .thenReturn(SessionResponse.authenticated(OTHER_ACCOUNT_ID, "b@example.com", "B"));
        when(shopRepository.findByAccountId(OTHER_ACCOUNT_ID)).thenReturn(Optional.empty());
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        shopService.saveOwnShop(SESSION_ID, "Tienda B", null);

        verify(shopRepository, never()).findByAccountId(ACCOUNT_ID);
        assertThat(captureSavedShop().getAccountId()).isEqualTo(OTHER_ACCOUNT_ID);
    }

    @Test
    void readsOwnShopWhenItExists() {
        authenticate(ACCOUNT_ID);
        Shop shop = Shop.open(ACCOUNT_ID, "Tienda", "Descripción", NOW);
        when(shopRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(shop));

        ShopResponse response = shopService.readOwnShop(SESSION_ID);

        assertThat(response).isEqualTo(new ShopResponse("Tienda", "Descripción"));
    }

    @Test
    void readsOwnShopReportsMissingShopWithoutSystemError() {
        authenticate(ACCOUNT_ID);
        when(shopRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.readOwnShop(SESSION_ID))
                .isInstanceOfSatisfying(ShopNotFoundException.class, exception -> {
                    assertThat(exception.getError()).isEqualTo("SHOP_NOT_FOUND");
                    assertThat(exception.getMessage()).isEqualTo("Todavía no configuraste tu tienda.");
                });
    }

    @Test
    void readsPublicShopExposingOnlyNameAndDescription() {
        Shop shop = Shop.open(ACCOUNT_ID, "Tienda", "Descripción", NOW);
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        when(shopRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(shop));

        ShopResponse response = shopService.readPublicShop(ACCOUNT_ID);

        assertThat(response).isEqualTo(new ShopResponse("Tienda", "Descripción"));
        assertThat(Arrays.stream(ShopResponse.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("name", "description");
    }

    @Test
    void readsPublicShopReportsVendorWithoutShop() {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        when(shopRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.readPublicShop(ACCOUNT_ID))
                .isInstanceOfSatisfying(ShopNotFoundException.class, exception -> {
                    assertThat(exception.getError()).isEqualTo("SHOP_NOT_FOUND");
                    assertThat(exception.getMessage()).isEqualTo("Ese vendedor todavía no tiene tienda.");
                });
    }

    @Test
    void readsPublicShopReportsUnknownAccount() {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> shopService.readPublicShop(ACCOUNT_ID))
                .isInstanceOfSatisfying(ShopNotFoundException.class, exception -> {
                    assertThat(exception.getError()).isEqualTo("ACCOUNT_NOT_FOUND");
                    assertThat(exception.getMessage()).isEqualTo("La cuenta indicada no existe.");
                });

        verifyNoInteractions(shopRepository);
    }

    private void authenticate(UUID accountId) {
        when(sessionService.read(SESSION_ID))
                .thenReturn(SessionResponse.authenticated(accountId, "ada@example.com", "Ada"));
    }

    private Shop captureSavedShop() {
        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());
        return captor.getValue();
    }
}
