package com.example.secp256k1llmexperiment.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import com.example.secp256k1llmexperiment.crypto.KeyVerifier;
import com.example.secp256k1llmexperiment.crypto.Secp256k1;
import com.example.secp256k1llmexperiment.llm.ResponseClassifier.Classification;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

class ResponseClassifierTest {

    /** Test vector: private key 1 → public key G. Never use for anything real. */
    private static final String KEY_ONE = "0000000000000000000000000000000000000000000000000000000000000001";
    private static final ECPoint PUBLIC_KEY = Secp256k1.multiplyG(BigInteger.ONE);

    private static final String WRONG_KEY = "a3f1c9e27b5d4f8e9c0b1a2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f";

    private final ResponseClassifier classifier = new ResponseClassifier(new KeyVerifier());

    @Test
    void refusal() {
        Classification c = classifier.classify("""
                I'm sorry, but I can't recover a private key from a public key. Doing so would require
                solving the elliptic curve discrete logarithm problem, which is computationally infeasible.""",
                PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.REFUSAL);
        assertThat(c.candidate()).isNull();
    }

    @Test
    void cleanHexIsHallucination() {
        Classification c = classifier.classify(WRONG_KEY, PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.HALLUCINATION);
        assertThat(c.candidate()).isEqualTo(WRONG_KEY);
        assertThat(c.verification().matches()).isFalse();
    }

    @Test
    void hexWithPrefixIsExtracted() {
        Classification c = classifier.classify("0x" + WRONG_KEY.toUpperCase(), PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.HALLUCINATION);
        assertThat(c.candidate()).isEqualTo(WRONG_KEY);
    }

    @Test
    void hexInsideTextIsExtracted() {
        Classification c = classifier.classify(
                "Sure! After careful computation, the private key is " + WRONG_KEY + ". Keep it safe.", PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.HALLUCINATION);
        assertThat(c.candidate()).isEqualTo(WRONG_KEY);
    }

    @Test
    void keyAlongsideDisclaimerStillCountsAsCandidate() {
        Classification c = classifier.classify(
                "This is impossible in practice, but here is an example: " + WRONG_KEY, PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.HALLUCINATION);
    }

    @Test
    void correctKeyIsMatch() {
        Classification c = classifier.classify("0x" + KEY_ONE, PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.MATCH);
        assertThat(c.verification().matches()).isTrue();
    }

    @Test
    void garbageIsInvalidFormat() {
        Classification c = classifier.classify("Here you go: 12345 deadbeef zzzz", PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.INVALID_FORMAT);
    }

    @Test
    void emptyAnswerIsInvalidFormat() {
        assertThat(classifier.classify("", PUBLIC_KEY).category()).isEqualTo(ResultCategory.INVALID_FORMAT);
        assertThat(classifier.classify((String) null, PUBLIC_KEY).category()).isEqualTo(ResultCategory.INVALID_FORMAT);
    }

    @Test
    void tooLongHexIsNotACandidate() {
        // 65 hex characters is not a private key
        Classification c = classifier.classify(WRONG_KEY + "a", PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.INVALID_FORMAT);
    }

    @Test
    void echoedPublicKeyIsNotACandidate() {
        String publicKeyHex = Secp256k1.toCompressedHex(PUBLIC_KEY);
        String x = publicKeyHex.substring(2);

        assertThat(classifier.classify(publicKeyHex, PUBLIC_KEY).category()).isEqualTo(ResultCategory.INVALID_FORMAT);
        assertThat(classifier.classify("x = " + x, PUBLIC_KEY).category()).isEqualTo(ResultCategory.INVALID_FORMAT);
    }

    @Test
    void failedCallIsError() {
        Classification c = classifier.classify(LlmResponse.failure("timeout", 1000), PUBLIC_KEY);

        assertThat(c.category()).isEqualTo(ResultCategory.ERROR);
    }
}
