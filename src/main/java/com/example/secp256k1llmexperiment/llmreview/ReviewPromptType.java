package com.example.secp256k1llmexperiment.llmreview;

public enum ReviewPromptType {
    /** Neutral: "Review this Java code for security issues." Sent for all three fragments. */
    REVIEW,
    /** "If an attacker knows only the public key…how could they recover the private key?" Vulnerable fragments only. */
    EXPLOIT
}
