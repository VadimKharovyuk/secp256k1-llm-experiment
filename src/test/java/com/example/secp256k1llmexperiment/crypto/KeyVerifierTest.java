package com.example.secp256k1llmexperiment.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import com.example.secp256k1llmexperiment.crypto.KeyVerifier.Verification;
import org.junit.jupiter.api.Test;

class KeyVerifierTest {

    private final KeyVerifier verifier = new KeyVerifier();
    private final GeneratedKeyPair keyPair = new KeyPairGenerator().generate();

    @Test
    void realPrivateKeyMatches() {
        Verification v = verifier.verify(keyPair.privateKey(), keyPair.publicKey());

        assertThat(v.inRange()).isTrue();
        assertThat(v.matches()).isTrue();
        assertThat(v.millis()).isNotNegative();
    }

    @Test
    void realPrivateKeyAsHexMatches() {
        String hex = Secp256k1.toHex64(keyPair.privateKey());

        assertThat(verifier.verify(hex, keyPair.publicKey()).matches()).isTrue();
    }

    @Test
    void privateKeyPlusOneDoesNotMatch() {
        BigInteger plusOne = keyPair.privateKey().add(BigInteger.ONE).mod(Secp256k1.N);
        if (plusOne.signum() == 0) {
            plusOne = BigInteger.ONE; // privateKey was n-1; astronomically unlikely
        }

        assertThat(verifier.verify(plusOne, keyPair.publicKey()).matches()).isFalse();
    }

    @Test
    void zeroIsRejected() {
        Verification v = verifier.verify(BigInteger.ZERO, keyPair.publicKey());

        assertThat(v.inRange()).isFalse();
        assertThat(v.matches()).isFalse();
    }

    @Test
    void curveOrderIsRejected() {
        Verification v = verifier.verify(Secp256k1.N, keyPair.publicKey());

        assertThat(v.inRange()).isFalse();
        assertThat(v.matches()).isFalse();
    }

    @Test
    void knownVectorPrivateKeyOneIsGenerator() {
        // 1·G = G, a well-known compressed public key
        assertThat(Secp256k1.toCompressedHex(Secp256k1.multiplyG(BigInteger.ONE)))
                .isEqualTo("0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");
    }

    @Test
    void generatedKeyIsCompressedAndInRange() {
        assertThat(keyPair.publicKeyHex()).hasSize(66).matches("0[23][0-9a-f]{64}");
        assertThat(Secp256k1.isValidPrivateKey(keyPair.privateKey())).isTrue();
        assertThat(keyPair.toString()).doesNotContain(keyPair.privateKey().toString(16));
    }
}
