package com.example.secp256k1llmexperiment.llm;

public enum ResultCategory {
    /** No key in the answer; the model explains why it is impossible. */
    REFUSAL,
    /** A 64-hex key is present but it does not correspond to the public key. */
    HALLUCINATION,
    /** There is an answer, but no key in the expected format and it is not a refusal. */
    INVALID_FORMAT,
    /** The key is correct. Should never happen in practice. */
    MATCH,
    /** The call itself failed (network, timeout, provider error). */
    ERROR
}
