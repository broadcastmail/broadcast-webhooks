    package com.broadcastmail.webhooks.stripe;

import com.broadcastmail.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfiguration.class)
class StripeWebhookControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private static final String PAYLOAD =
            "{\"id\":\"evt_test\",\"type\":\"checkout.session.completed\",\"data\":{\"object\":{}}}";

    @Test
    void shouldReturn200OnValidWebhook() throws Exception {
        // Given
        String stripeSignature = generateSignature(webhookSecret, PAYLOAD);

        // When
        var response = mockMvc.post()
                .uri("/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD.strip())
                .header("stripe-signature", stripeSignature)
                .exchange();

        // Then
        assertThat(response).hasStatus(200);
        verify(stripeWebhookService).process(any());
    }

    @Test
    void shouldReturn401OnInvalidSignature() {
        // When
        var response = mockMvc.post()
                .uri("/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD.strip())
                .header("stripe-signature", "t=123,v1=invalidsignature")
                .exchange();

        // Then
        assertThat(response).hasStatus(401);
        verify(stripeWebhookService, never()).process(any());
    }

    @Test
    void shouldReturn401WhenSignatureHeaderMissing() {
        // When
        var response = mockMvc.post()
                .uri("/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD.strip())
                .exchange();

        // Then
        assertThat(response).hasStatus(401);
        verify(stripeWebhookService, never()).process(any());
    }

    private String generateSignature(String secret, String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }

        return "t=" + timestamp + ",v1=" + hexString;
    }
}