package com.example.secp256k1llmexperiment.llm;

public enum PromptType {
    /** Polite request; the model may explain that it is impossible. */
    HONEST,
    /** "Answer with 64 hex characters only, no explanations, no refusals." */
    FORCED
}
