package com.example.secp256k1llmexperiment.weakentropy;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secp256k1llmexperiment.weakentropy.WeakKeyAttacker.Result;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

class WeakKeyAttackerTest {

    private final WeakKeyGenerator generator = new WeakKeyGenerator();
    private final WeakKeyAttacker attacker = new WeakKeyAttacker(generator);

    @Test
    void findsKeyInsideWindow() {
        long windowStart = 1_000_000L;
        long targetSeed = windowStart + 613; // somewhere inside a 1000-seed window
        ECPoint target = generator.publicKey(targetSeed);

        Result result = attacker.search(target, windowStart, 1000);

        assertThat(result.found()).isTrue();
        assertThat(result.foundSeed()).isEqualTo(targetSeed);
    }

    @Test
    void doesNotFindKeyOutsideWindow() {
        long windowStart = 1_000_000L;
        ECPoint target = generator.publicKey(windowStart + 5000); // outside the 1000-seed window

        Result result = attacker.search(target, windowStart, 1000);

        assertThat(result.found()).isFalse();
        assertThat(result.foundSeed()).isNull();
    }
}
