package com.example.secp256k1llmexperiment.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code llm-review.*} in application.yml. Models, temperature and the default token budget are
 * reused from {@code experiment.*}; only review-specific settings live here.
 *
 * @param runsPerPrompt  runs per (model × fragment × prompt)
 * @param modelMaxTokens per-model token budget override (deepseek needs more room than 2048,
 *                       otherwise the answer is lost to hidden reasoning)
 */
@ConfigurationProperties(prefix = "llm-review")
public record LlmReviewProperties(
        @DefaultValue("3") int runsPerPrompt,
        Map<String, Integer> modelMaxTokens) {

    public LlmReviewProperties {
        if (runsPerPrompt < 1) {
            throw new IllegalArgumentException("llm-review.runs-per-prompt must be >= 1");
        }
        modelMaxTokens = modelMaxTokens == null ? Map.of() : Map.copyOf(modelMaxTokens);
    }

    public int maxTokensFor(String model, int defaultMaxTokens) {
        return modelMaxTokens.getOrDefault(model, defaultMaxTokens);
    }
}
