package com.example.secp256k1llmexperiment.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code weak-entropy.*} in application.yml: which scenarios to run, the search-window sizes,
 * JVM warm-up, and the assumptions behind the 2^32 extrapolation.
 *
 * @param scenarios        scenario names to run (see {@code WeakEntropyScenario})
 * @param secondsWindow    candidates for the SECONDS_DAY scenario (one day = 86 400)
 * @param millisWindow     candidates for the MILLIS_HOUR scenario (one hour = 3 600 000)
 * @param warmupCandidates derivations done before timing, so the JIT has compiled the hot path
 * @param benchmarkCandidates derivations used to measure candidates/second
 * @param extrapolation    assumptions for the "how long on N GPUs" estimate of a full 2^32 search
 */
@ConfigurationProperties(prefix = "weak-entropy")
public record WeakEntropyProperties(
        @DefaultValue({"SECONDS_DAY", "MILLIS_HOUR", "SEED_32BIT", "SECURE_RANDOM"}) List<String> scenarios,
        @DefaultValue("86400") long secondsWindow,
        @DefaultValue("3600000") long millisWindow,
        @DefaultValue("50000") int warmupCandidates,
        @DefaultValue("200000") int benchmarkCandidates,
        @DefaultValue Extrapolation extrapolation) {

    public WeakEntropyProperties {
        if (scenarios == null || scenarios.isEmpty()) {
            throw new IllegalArgumentException("weak-entropy.scenarios must not be empty");
        }
        scenarios = List.copyOf(scenarios);
    }

    /**
     * @param gpuCount                   number of GPUs assumed for the extrapolation
     * @param candidatesPerSecondPerGpu  weak-key candidates one GPU could test per second
     */
    public record Extrapolation(
            @DefaultValue("100") BigDecimal gpuCount,
            @DefaultValue("1e9") BigDecimal candidatesPerSecondPerGpu) {
    }
}
