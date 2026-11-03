package com.acme.wallet;

import java.math.BigInteger;
import java.util.Random;

import org.bouncycastle.math.ec.ECPoint;

public class WalletKeyService {

    private final ECPoint generator;
    private final BigInteger order;

    public WalletKeyService(ECPoint generator, BigInteger order) {
        this.generator = generator;
        this.order = order;
    }

    public BigInteger newPrivateKey() {
        Random random = new Random(System.currentTimeMillis());
        byte[] material = new byte[32];
        random.nextBytes(material);
        return new BigInteger(1, material).mod(order.subtract(BigInteger.ONE)).add(BigInteger.ONE);
    }

    public ECPoint publicKey(BigInteger privateKey) {
        return generator.multiply(privateKey).normalize();
    }
}
