package com.example.secp256k1llmexperiment.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Component;

/**
 * Generates a fresh secp256k1 key pair on every run. The key has never held any funds.
 */
@Component
public class KeyPairGenerator {

    private final SecureRandom random;

    public KeyPairGenerator() {
        this(new SecureRandom());
    }

    KeyPairGenerator(SecureRandom random) {
        this.random = random;
    }

    public GeneratedKeyPair generate() {
        BigInteger privateKey;
        do {
            privateKey = new BigInteger(Secp256k1.N.bitLength(), random);
        } while (!Secp256k1.isValidPrivateKey(privateKey)); // rejection sampling into [1, n-1]

        long start = System.nanoTime();
        ECPoint publicKey = Secp256k1.multiplyG(privateKey);
        double millis = (System.nanoTime() - start) / 1_000_000.0;

        return new GeneratedKeyPair(privateKey, publicKey, Secp256k1.toCompressedHex(publicKey), millis);
    }
}
