package com.friendlyeshop.account.auth.google.application;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class FakeGoogleAuthClientTest {
    @Test
    void simulatesSuccessAndFailureWithoutNetwork() {
        FakeGoogleAuthClient client = new FakeGoogleAuthClient();
        client.succeedWith(new GoogleAuthClient.GoogleIdentity("sub", "a@example.com", "Ada"));

        assertThat(client.authorizationUrl("state-1")).contains("state=state-1");
        assertThat(client.lastState()).isEqualTo("state-1");
        assertThat(client.exchangeCode("ok")).isPresent().get()
                .extracting(GoogleAuthClient.GoogleIdentity::subject)
                .isEqualTo("sub");

        client.failNextExchange();
        assertThat(client.exchangeCode("ok")).isEmpty();
        assertThat(client.exchangeCode("cancel")).isEmpty();
    }
}
