package com.example.secp256k1llmexperiment.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code experiment.*} in application.yml.
 *
 * @param models              OpenRouter model IDs to query
 * @param runsPerPrompt       how many times each model × prompt pair is asked
 * @param temperature         sampling temperature, the same for every call
 * @param maxTokens           upper bound for one answer (keeps the run cheap)
 * @param reasoningMaxTokens  per-model cap on hidden reasoning (OpenRouter {@code reasoning.max_tokens});
 *                            without it a reasoning model can spend all of {@code maxTokens} thinking
 *                            and return an empty answer
 * @param resultsDir          where results/&lt;timestamp&gt;/ is created
 */
@ConfigurationProperties(prefix = "experiment")
public record ExperimentProperties(
        List<String> models,
        @DefaultValue("3") int runsPerPrompt,
        @DefaultValue("0.7") double temperature,
        @DefaultValue("2048") int maxTokens,
        Map<String, Integer> reasoningMaxTokens,
        @DefaultValue("results") String resultsDir) {

    public ExperimentProperties {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("experiment.models must contain at least one model ID");
        }
        if (runsPerPrompt < 1) {
            throw new IllegalArgumentException("experiment.runs-per-prompt must be >= 1");
        }
        models = List.copyOf(models);
        reasoningMaxTokens = reasoningMaxTokens == null ? Map.of() : Map.copyOf(reasoningMaxTokens);
        reasoningMaxTokens.forEach((model, budget) -> {
            if (budget >= maxTokens) {
                throw new IllegalArgumentException("experiment.reasoning-max-tokens for " + model
                        + " must be less than experiment.max-tokens, otherwise nothing is left for the answer");
            }
        });
    }
}
