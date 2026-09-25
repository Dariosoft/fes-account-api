package com.friendlyeshop.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private GoogleIdentityService googleIdentityService;

    @Mock
    private AccountService accountService;

    @Mock
    private SessionService sessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SessionCookieSupport cookies = new SessionCookieSupport(new SessionCookieProperties(false, "Lax"));
        GoogleAuthProperties googleProps = new GoogleAuthProperties(
                "client-id", "secret", "http://localhost/callback", "https://accounts.google.com");
        StoreAuthController authController = new StoreAuthController(
                googleIdentityService, accountService, sessionService, cookies, googleProps);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void googleAuthFailureReturnsSpanishMessageWithoutSecrets() throws Exception {
        when(googleIdentityService.verifyIdToken("leaked-secret-token"))
                .thenThrow(new GoogleAuthenticationException("No se pudo validar la identidad con Google."));

        MvcResult result = mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"leaked-secret-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No se pudo validar la identidad con Google."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("leaked-secret-token");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret");
        verify(sessionService, never()).create(any());
    }

    @Test
    void invalidPayloadReturnsSpanishValidationMessage() throws Exception {
        mockMvc.perform(post("/accounts/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Los datos enviados no son válidos."));
    }
}
