package com.example.secp256k1llmexperiment.estimate;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.example.secp256k1llmexperiment.config.EstimatorProperties;
import com.example.secp256k1llmexperiment.estimate.BruteForceEstimator.Estimate;
import org.junit.jupiter.api.Test;

class BruteForceEstimatorTest {

    private static final MathContext SIX_DIGITS = new MathContext(6);

    /** Same assumptions as application.yml: 10^10 ops/s per GPU, 10^9 GPUs. */
    private final BruteForceEstimator estimator =
            new BruteForceEstimator(new EstimatorProperties(new BigDecimal("1e9"), new BigDecimal("1e10")));

    @Test
    void operationsAreTwoToThe128() {
        assertThat(estimator.estimate().operations())
                .isEqualTo(new BigInteger("340282366920938463463374607431768211456"));
    }

    @Test
    void matchesHandCalculation() {
        // By hand:
        // 2^128                    = 3.40282366920938463e38 operations
        // one GPU: / 1e10 ops/s    = 3.40282366920938463e28 s
        //          / 31 557 600    = 1.07828975245563e21 years
        // all GPUs: / 1e9          = 1.07828975245563e12 years
        // / 1.38e10 (universe age) = 78.1369385837
        Estimate e = estimator.estimate(BigInteger.TWO.pow(128));

        assertThat(e.yearsOneGpu().round(SIX_DIGITS)).isEqualByComparingTo("1.07829e21");
        assertThat(e.yearsAllGpus().round(SIX_DIGITS)).isEqualByComparingTo("1.07829e12");
        assertThat(e.allGpusInUniverseAges().round(SIX_DIGITS)).isEqualByComparingTo("78.1369");
    }

    @Test
    void assumptionsAreReturnedWithTheResult() {
        Estimate e = estimator.estimate();

        assertThat(e.gpuCount()).isEqualByComparingTo("1e9");
        assertThat(e.opsPerSecondPerGpu()).isEqualByComparingTo("1e10");
    }
}
