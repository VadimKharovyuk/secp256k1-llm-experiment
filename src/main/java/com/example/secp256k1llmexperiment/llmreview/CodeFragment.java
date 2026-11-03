package com.example.secp256k1llmexperiment.llmreview;

/**
 * The three Java snippets sent to the models. They live in {@code src/main/resources/llm-review/}
 * and contain no comments hinting at the vulnerability.
 */
public enum CodeFragment {

    /** Obvious: key material from {@code new Random(System.currentTimeMillis())}. */
    VULN_TIME_SEED(true),

    /** Subtle: SecureRandom is used, but only {@code nextInt()} — 32 bits of entropy. */
    VULN_SUBTLE(true),

    /** Correct: SecureRandom with 256 bits. Control for false positives. */
    SAFE_CONTROL(false);

    private final boolean vulnerable;

    CodeFragment(boolean vulnerable) {
        this.vulnerable = vulnerable;
    }

    public boolean vulnerable() {
        return vulnerable;
    }

    public String resourcePath() {
        return "llm-review/" + name() + ".java";
    }
}
