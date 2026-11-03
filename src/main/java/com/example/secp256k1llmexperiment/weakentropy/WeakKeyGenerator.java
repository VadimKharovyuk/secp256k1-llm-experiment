package com.example.secp256k1llmexperiment.weakentropy;

import java.math.BigInteger;
import java.util.Random;

import com.example.secp256k1llmexperiment.crypto.Secp256k1;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Component;

/**
 * Deliberately WEAK, educational key generator — not a wallet library, never use it for real keys.
 * <p>
 * It mirrors the real-world "Milk Sad" class of bug (2023): the private key comes from
 * {@link java.util.Random} seeded with a low-entropy value. {@code Random} is fully determined by
 * its 48-bit seed, so if the attacker can guess the seed, they reproduce the exact private key.
 * <p>
 * The derivation is a pure function of the seed, so the target and the attacker compute the same
 * key for the same seed.
 */
@Component
public class WeakKeyGenerator {

    private static final BigInteger N_MINUS_ONE = Secp256k1.N.subtract(BigInteger.ONE);

    /** seed → {@code new Random(seed).nextBytes(32)} → scalar in [1, n-1]. */
    public BigInteger privateKey(long seed) {
        byte[] material = new byte[32];
        new Random(seed).nextBytes(material);
        return new BigInteger(1, material).mod(N_MINUS_ONE).add(BigInteger.ONE);
    }

    public ECPoint publicKey(long seed) {
        return Secp256k1.multiplyG(privateKey(seed));
    }
}
