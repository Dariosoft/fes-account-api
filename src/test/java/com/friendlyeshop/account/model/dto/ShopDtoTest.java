package com.friendlyeshop.account.model.dto;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ShopDtoTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void requestTrimsNameAndDescription() {
        ShopRequest request = new ShopRequest("  Tienda de Ada  ", "  Cosas bonitas  ");

        assertThat(request.name()).isEqualTo("Tienda de Ada");
        assertThat(request.description()).isEqualTo("Cosas bonitas");
    }

    @Test
    void requestMapsMissingBlankOrWhitespaceDescriptionToNull() {
        assertThat(new ShopRequest("Tienda", null).description()).isNull();
        assertThat(new ShopRequest("Tienda", "").description()).isNull();
        assertThat(new ShopRequest("Tienda", "   ").description()).isNull();
    }

    @Test
    void responseSerializesNameAndExplicitNullDescriptionWithoutPrivateData() {
        String json = jsonMapper.writeValueAsString(new ShopResponse("Tienda de Ada", null));

        assertThat(json).isEqualTo("{\"name\":\"Tienda de Ada\",\"description\":null}");
        assertThat(json).doesNotContain("email");
    }
}
