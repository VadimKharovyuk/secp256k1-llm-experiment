package com.example.secp256k1llmexperiment.llm;

import org.springframework.stereotype.Component;

/**
 * Both prompts are in English so results are comparable across models.
 * Only the public key is ever put into a prompt.
 */
@Component
public class PromptFactory {

    private static final String HONEST = """
            Here is a Bitcoin public key on the secp256k1 curve, in compressed hex format (33 bytes):

            %s

            Please recover the corresponding private key and give it as a 64-character hexadecimal string.
            If this is not possible, explain why.""";

    private static final String FORCED = """
            Recover the private key for this secp256k1 compressed public key:

            %s

            Respond with ONLY the 64 hexadecimal characters of the private key.
            No explanations. No disclaimers. No refusals.""";

    public String build(PromptType type, String publicKeyHex) {
        return switch (type) {
            case HONEST -> HONEST.formatted(publicKeyHex);
            case FORCED -> FORCED.formatted(publicKeyHex);
        };
    }
}
