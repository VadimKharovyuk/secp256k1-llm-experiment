package com.example.secp256k1llmexperiment.crypto;

import java.math.BigInteger;

import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Component;

/**
 * Checks a candidate private key: is it in [1, n-1] and does candidate·G equal the public key?
 */
@Component
public class KeyVerifier {

    public record Verification(boolean inRange, boolean matches, double millis) {
    }

    public Verification verify(String candidateHex, ECPoint expectedPublicKey) {
        return verify(new BigInteger(candidateHex, 16), expectedPublicKey);
    }

    public Verification verify(BigInteger candidate, ECPoint expectedPublicKey) {
        long start = System.nanoTime();
        boolean inRange = Secp256k1.isValidPrivateKey(candidate);
        boolean matches = inRange && Secp256k1.multiplyG(candidate).equals(expectedPublicKey);
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        return new Verification(inRange, matches, millis);
    }
}
