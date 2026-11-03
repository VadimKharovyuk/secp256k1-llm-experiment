package com.example.secp256k1llmexperiment.estimate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.example.secp256k1llmexperiment.config.EstimatorProperties;
import org.springframework.stereotype.Component;

/**
 * How long would it take to find a private key by brute force?
 * <p>
 * The best known generic attack on secp256k1 is Pollard's rho, which needs about √n ≈ 2^128
 * group operations (more precisely √(πn/4) ≈ 0.89 · 2^128; we use 2^128 as the round figure).
 */
@Component
public class BruteForceEstimator {

    /** √n for secp256k1, rounded to a power of two. */
    public static final BigInteger POLLARD_RHO_OPERATIONS = BigInteger.TWO.pow(128);

    /** Julian year, 365.25 days. */
    public static final BigDecimal SECONDS_PER_YEAR = new BigDecimal("31557600");

    /** ~13.8 billion years. */
    public static final BigDecimal AGE_OF_UNIVERSE_YEARS = new BigDecimal("1.38E10");

    private static final MathContext MC = MathContext.DECIMAL128;

    public record Estimate(
            BigInteger operations,
            BigDecimal gpuCount,
            BigDecimal opsPerSecondPerGpu,
            BigDecimal yearsOneGpu,
            BigDecimal yearsAllGpus,
            BigDecimal allGpusInUniverseAges) {
    }

    private final EstimatorProperties properties;

    public BruteForceEstimator(EstimatorProperties properties) {
        this.properties = properties;
    }

    public Estimate estimate() {
        return estimate(POLLARD_RHO_OPERATIONS);
    }

    public Estimate estimate(BigInteger operations) {
        BigDecimal ops = new BigDecimal(operations);
        BigDecimal perGpu = properties.opsPerSecondPerGpu();
        BigDecimal gpus = properties.gpuCount();

        BigDecimal yearsOneGpu = ops.divide(perGpu, MC).divide(SECONDS_PER_YEAR, MC);
        BigDecimal yearsAllGpus = yearsOneGpu.divide(gpus, MC);
        BigDecimal universeAges = yearsAllGpus.divide(AGE_OF_UNIVERSE_YEARS, MC);

        return new Estimate(operations, gpus, perGpu, yearsOneGpu, yearsAllGpus, universeAges);
    }
}
