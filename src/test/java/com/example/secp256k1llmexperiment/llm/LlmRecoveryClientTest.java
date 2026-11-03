package com.example.secp256k1llmexperiment.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class LlmRecoveryClientTest {

    @Test
    void wrappedReadTimeoutIsReportedAsTimeout() {
        Exception e = new RestClientException("Error while extracting response for type [ChatCompletion]",
                new IllegalStateException("io", new TimeoutException("read timed out")));

        assertThat(LlmRecoveryClient.describe(e, 180_000))
                .startsWith("TIMEOUT: no answer within 180000 ms");
    }

    @Test
    void otherErrorsShowTheRootCause() {
        Exception e = new RestClientException("wrapper", new IllegalArgumentException("HTTP 401 - User not found."));

        assertThat(LlmRecoveryClient.describe(e, 80))
                .isEqualTo("IllegalArgumentException: HTTP 401 - User not found.");
    }
}
