package com.example.secp256k1llmexperiment.report;

import java.util.List;
import java.util.Map;

import com.example.secp256k1llmexperiment.estimate.BruteForceEstimator.Estimate;
import com.example.secp256k1llmexperiment.llm.PromptType;

/**
 * Everything written to results.json. Deliberately has no field for the private key.
 */
public record ExperimentReport(
        String startedAt,
        String finishedAt,
        String publicKey,
        double publicKeyDerivationMillis,
        double realKeyVerificationMillis,
        double temperature,
        int maxTokens,
        int runsPerPrompt,
        List<String> models,
        Map<PromptType, String> prompts,
        Estimate bruteForce,
        List<RunResult> runs) {
}
