package com.example.secp256k1llmexperiment.crypto;

import java.math.BigInteger;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.util.encoders.Hex;

/**
 * The secp256k1 curve used by Bitcoin: y² = x³ + 7 over a 256-bit prime field.
 */
public final class Secp256k1 {

    private static final X9ECParameters PARAMS = CustomNamedCurves.getByName("secp256k1");

    public static final ECCurve CURVE = PARAMS.getCurve();

    /** Generator point G. */
    public static final ECPoint G = PARAMS.getG();

    /** Order n of G: valid private keys are 1 … n-1. */
    public static final BigInteger N = PARAMS.getN();

    private Secp256k1() {
    }

    /** k·G — the only "hard" step of turning a private key into a public key. */
    public static ECPoint multiplyG(BigInteger k) {
        return new FixedPointCombMultiplier().multiply(G, k).normalize();
    }

    public static boolean isValidPrivateKey(BigInteger k) {
        return k.signum() > 0 && k.compareTo(N) < 0;
    }

    /** 33-byte compressed encoding (02/03 prefix + x), as hex. */
    public static String toCompressedHex(ECPoint point) {
        return Hex.toHexString(point.getEncoded(true));
    }

    public static ECPoint decodePoint(String hex) {
        return CURVE.decodePoint(Hex.decode(hex));
    }

    /** Private key as 64 lowercase hex characters (zero-padded). */
    public static String toHex64(BigInteger k) {
        return String.format("%064x", k);
    }
}
