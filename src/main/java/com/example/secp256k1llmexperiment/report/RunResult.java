package com.example.secp256k1llmexperiment.report;

import com.example.secp256k1llmexperiment.llm.PromptType;
import com.example.secp256k1llmexperiment.llm.ResultCategory;

/**
 * One model × prompt × run. {@code rawResponse} is stored exactly as the model returned it.
 */
public record RunResult(
        String model,
        PromptType prompt,
        int run,
        ResultCategory category,
        String candidate,
        Boolean candidateInRange,
        Double verificationMillis,
        String rawResponse,
        String finishReason,
        Integer totalTokens,
        long latencyMs,
        String error) {
}
