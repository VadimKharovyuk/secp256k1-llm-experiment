package com.example.secp256k1llmexperiment.weakentropy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import com.example.secp256k1llmexperiment.crypto.Secp256k1;
import org.junit.jupiter.api.Test;

class WeakKeyGeneratorTest {

    private final WeakKeyGenerator generator = new WeakKeyGenerator();

    @Test
    void sameSeedGivesSameKey() {
        assertThat(generator.privateKey(12345L)).isEqualTo(generator.privateKey(12345L));
        assertThat(generator.publicKey(12345L)).isEqualTo(generator.publicKey(12345L));
    }

    @Test
    void differentSeedsGiveDifferentKeys() {
        assertThat(generator.privateKey(1L)).isNotEqualTo(generator.privateKey(2L));
    }

    @Test
    void keyIsInRange() {
        for (long seed : new long[] {0L, 1L, -1L, Long.MAX_VALUE, System.currentTimeMillis()}) {
            BigInteger key = generator.privateKey(seed);
            assertThat(Secp256k1.isValidPrivateKey(key))
                    .as("seed %d must yield a key in [1, n-1]", seed)
                    .isTrue();
        }
    }

    @Test
    void publicKeyMatchesPrivateKey() {
        long seed = 987654321L;
        assertThat(generator.publicKey(seed)).isEqualTo(Secp256k1.multiplyG(generator.privateKey(seed)));
    }
}
