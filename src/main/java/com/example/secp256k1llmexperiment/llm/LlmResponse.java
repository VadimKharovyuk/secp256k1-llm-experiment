package com.example.secp256k1llmexperiment.llm;

/**
 * Raw outcome of one call. Exactly one of {@code text} / {@code error} is meaningful.
 *
 * @param text         answer exactly as returned by the model (may be empty)
 * @param finishReason e.g. "stop" or "length" (null if unknown)
 * @param totalTokens  prompt + completion tokens reported by the provider (null if unknown)
 * @param latencyMs    wall-clock time of the call
 * @param error        exception summary if the call failed, otherwise null
 */
public record LlmResponse(String text, String finishReason, Integer totalTokens, long latencyMs, String error) {

    public boolean failed() {
        return error != null;
    }

    public static LlmResponse failure(String error, long latencyMs) {
        return new LlmResponse(null, null, null, latencyMs, error);
    }
}
