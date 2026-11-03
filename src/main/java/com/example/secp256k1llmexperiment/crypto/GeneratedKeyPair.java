package com.example.secp256k1llmexperiment.crypto;

import java.math.BigInteger;

import org.bouncycastle.math.ec.ECPoint;

/**
 * One-time key pair. The private key stays on this machine: only {@link #publicKeyHex()}
 * is ever sent to an LLM.
 */
public record GeneratedKeyPair(BigInteger privateKey, ECPoint publicKey, String publicKeyHex, double publicKeyMillis) {

    /** Never print the private key by accident (records include every field in toString by default). */
    @Override
    public String toString() {
        return "GeneratedKeyPair[publicKeyHex=" + publicKeyHex + ", privateKey=<redacted>]";
    }
}
