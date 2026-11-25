package com.friendlyeshop.account.model.dto;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyeshop.account.model.dto.out.ShopResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ShopDtoTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void responseSerializesNameAndExplicitNullDescriptionWithoutPrivateData() {
        String json = jsonMapper.writeValueAsString(new ShopResponse("Tienda de Ada", null));

        assertThat(json).isEqualTo("{\"name\":\"Tienda de Ada\",\"description\":null}");
        assertThat(json).doesNotContain("email");
    }
}
