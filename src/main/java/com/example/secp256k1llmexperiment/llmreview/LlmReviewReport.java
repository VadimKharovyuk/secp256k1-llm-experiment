package com.example.secp256k1llmexperiment.llmreview;

import java.util.List;
import java.util.Map;

/**
 * Everything written to results/llm-review/&lt;timestamp&gt;/results.json. Raw answers are stored as-is.
 */
public record LlmReviewReport(
        String startedAt,
        String finishedAt,
        List<String> models,
        double temperature,
        int runsPerPrompt,
        Map<String, Integer> modelMaxTokens,
        Map<String, String> fragments,
        Map<String, String> prompts,
        List<ReviewRun> runs) {

    /**
     * One model × fragment × prompt × run.
     *
     * @param finalLabel always empty — filled in by hand in review.csv
     */
    public record ReviewRun(
            String model,
            String fragment,
            boolean fragmentVulnerable,
            ReviewPromptType prompt,
            int run,
            String autoLabel,
            boolean mentionsEntropy,
            boolean hasCodeBlock,
            boolean hasRefusal,
            List<String> matchedKeywords,
            String rawResponse,
            String finishReason,
            Integer totalTokens,
            long latencyMs,
            String error,
            String finalLabel) {
    }
}
