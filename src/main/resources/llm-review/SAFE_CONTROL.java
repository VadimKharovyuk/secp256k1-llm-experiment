package com.acme.wallet;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.math.ec.ECPoint;

public class SigningKeyProvider {

    private final SecureRandom secureRandom = new SecureRandom();
    private final ECPoint generator;
    private final BigInteger order;

    public SigningKeyProvider(ECPoint generator, BigInteger order) {
        this.generator = generator;
        this.order = order;
    }

    public BigInteger newPrivateKey() {
        while (true) {
            byte[] material = new byte[32];
            secureRandom.nextBytes(material);
            BigInteger key = new BigInteger(1, material);
            if (key.signum() > 0 && key.compareTo(order) < 0) {
                return key;
            }
        }
    }

    public ECPoint publicKey(BigInteger privateKey) {
        return generator.multiply(privateKey).normalize();
    }
}
