package com.example.secp256k1llmexperiment.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code experiment.*} in application.yml.
 *
 * @param models        OpenRouter model IDs to query
 * @param runsPerPrompt how many times each model × prompt pair is asked
 * @param temperature   sampling temperature, the same for every call
 * @param maxTokens     upper bound for one answer (keeps the run cheap)
 * @param resultsDir    where results/&lt;timestamp&gt;/ is created
 */
@ConfigurationProperties(prefix = "experiment")
public record ExperimentProperties(
        List<String> models,
        @DefaultValue("3") int runsPerPrompt,
        @DefaultValue("0.7") double temperature,
        @DefaultValue("2048") int maxTokens,
        @DefaultValue("results") String resultsDir) {

    public ExperimentProperties {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("experiment.models must contain at least one model ID");
        }
        if (runsPerPrompt < 1) {
            throw new IllegalArgumentException("experiment.runs-per-prompt must be >= 1");
        }
        models = List.copyOf(models);
    }
}
