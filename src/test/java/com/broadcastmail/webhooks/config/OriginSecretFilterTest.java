package com.broadcastmail.webhooks.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OriginSecretFilterTest {

    @Test
    void withoutAConfiguredSecretEveryRequestPasses() throws Exception {
        // Given
        OriginSecretFilter filter = new OriginSecretFilter("");

        // When
        MockHttpServletResponse response = run(filter, "/anything", null);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void aRequestWithTheSecretPasses() throws Exception {
        // Given
        OriginSecretFilter filter = new OriginSecretFilter("s3cret");

        // When
        MockHttpServletResponse response = run(filter, "/anything", "s3cret");

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void aRequestWithoutOrWithAWrongSecretIsRefused() throws Exception {
        // Given
        OriginSecretFilter filter = new OriginSecretFilter("s3cret");

        // When
        MockHttpServletResponse missing = run(filter, "/anything", null);
        MockHttpServletResponse wrong = run(filter, "/anything", "guess");

        // Then
        assertThat(missing.getStatus()).isEqualTo(403);
        assertThat(wrong.getStatus()).isEqualTo(403);
    }

    @Test
    void theHealthCheckNeedsNoSecret() throws Exception {
        // Given
        OriginSecretFilter filter = new OriginSecretFilter("s3cret");

        // When
        MockHttpServletResponse response = run(filter, "/actuator/health", null);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletResponse run(OriginSecretFilter filter, String path, String secret) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (secret != null) request.addHeader(OriginSecretFilter.HEADER, secret);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
