package com.friendlyeshop.account.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AccountControllerTest {
    @Test
    void identifiesService() {
        assertThat(new AccountController().accounts())
                .containsEntry("service", "account-api")
                .containsEntry("status", "ready")
                .containsKey("accounts");
    }
}
