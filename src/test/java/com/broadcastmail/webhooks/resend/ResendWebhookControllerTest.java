package com.broadcastmail.webhooks.resend;

import com.broadcastmail.TestContainersConfiguration;
import com.broadcastmail.common.emailprovider.EmailProvider;
import com.broadcastmail.common.emailprovider.EmailProviderRepository;
import com.broadcastmail.webhooks.common.SecurityUtil;
import com.broadcastmail.webhooks.config.EncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfiguration.class)

class ResendWebhookControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private EmailProviderRepository emailProviderRepository;

    @MockitoBean
    private EncryptionProperties encryptionProperties;

    @MockitoBean
    private WebhookSignatureVerifier signatureVerifier;

    @MockitoBean
    private ResendWebhookService resendWebhookService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String PAYLOAD = """
            {"type":"email.delivered","created_at":"2026-08-05T10:00:00Z","data":{"email_id":"msg-123"}}
            """;

    @BeforeEach
    void setUp() {
        String encryptedSecret = SecurityUtil.encrypt("whsec_test123", "12345678901234567890123456789012");
        EmailProvider provider = EmailProvider.builder()
                .accountId(ACCOUNT_ID)
                .encryptedWebhookSecret(encryptedSecret)
                .build();
        when(emailProviderRepository.findByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(provider));
        when(encryptionProperties.key()).thenReturn("12345678901234567890123456789012");
    }

    @Test
    void shouldReturn200OnValidWebhook() {
        // When
        var response = mockMvc.post()
                .uri("/webhooks/resend/" + ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD)
                .header("svix-id", "msg-123")
                .exchange();

        // Then
        assertThat(response).hasStatus(200);
        verify(resendWebhookService).process("msg-123", PAYLOAD);
    }

    @Test
    void shouldReturn401OnInvalidSignature() {
        // Given
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature"))
                .when(signatureVerifier).verify(any(), any(), any());

        // When
        var response = mockMvc.post()
                .uri("/webhooks/resend/" + ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD)
                .header("svix-id", "msg-123")
                .exchange();

        // Then
        assertThat(response).hasStatus(401);
        verify(resendWebhookService, never()).process(any(), any());
    }

    @Test
    void shouldReturn401WhenNoWebhookSecretRegistered() {
        // Given
        EmailProvider providerWithoutSecret = EmailProvider.builder()
                .accountId(ACCOUNT_ID)
                .encryptedWebhookSecret(null)
                .build();
        when(emailProviderRepository.findByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(providerWithoutSecret));

        // When
        var response = mockMvc.post()
                .uri("/webhooks/resend/" + ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD)
                .header("svix-id", "msg-123")
                .exchange();

        // Then
        assertThat(response).hasStatus(401);
        verify(resendWebhookService, never()).process(any(), any());
    }
}