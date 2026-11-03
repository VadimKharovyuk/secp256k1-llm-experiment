package com.acme.wallet;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.math.ec.ECPoint;

public class TokenKeyFactory {

    private static final long MIX = 0x9E3779B97F4A7C15L;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ECPoint generator;
    private final BigInteger order;

    public TokenKeyFactory(ECPoint generator, BigInteger order) {
        this.generator = generator;
        this.order = order;
    }

    public BigInteger derivePrivateKey() {
        int entropy = secureRandom.nextInt();
        BigInteger seed = BigInteger.valueOf(entropy & 0xFFFFFFFFL);
        return hashToScalar(seed);
    }

    private BigInteger hashToScalar(BigInteger seed) {
        BigInteger scalar = seed.multiply(BigInteger.valueOf(MIX)).mod(order);
        return scalar.signum() == 0 ? BigInteger.ONE : scalar;
    }

    public ECPoint publicKey(BigInteger privateKey) {
        return generator.multiply(privateKey).normalize();
    }
}
