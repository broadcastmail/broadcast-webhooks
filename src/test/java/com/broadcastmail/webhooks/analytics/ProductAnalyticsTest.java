package com.broadcastmail.webhooks.analytics;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ProductAnalyticsTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private HttpServer server;
    private final List<String> bodies = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/i/v0/e/", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void shouldSendEventKeyedByAccountId() {
        // Given
        ProductAnalytics analytics = new ProductAnalytics(
                "phc_test", "http://127.0.0.1:" + server.getAddress().getPort(), Set.of());

        // When
        analytics.capture(ACCOUNT_ID, "plan_upgraded", "evt_1", 1_800_000_000L);

        // Then
        assertThat(bodies).singleElement().asString()
                .contains("\"event\":\"plan_upgraded\"")
                .contains("\"distinct_id\":\"" + ACCOUNT_ID + "\"")
                .contains("\"api_key\":\"phc_test\"");
    }
}
