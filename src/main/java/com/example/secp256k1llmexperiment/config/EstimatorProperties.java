package com.example.secp256k1llmexperiment.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code estimator.*} in application.yml: the assumptions behind the brute-force estimate.
 * They are printed next to the result so the reader can see what the numbers rest on.
 *
 * @param gpuCount           number of GPUs in the world
 * @param opsPerSecondPerGpu elliptic curve group operations per second on one GPU
 */
@ConfigurationProperties(prefix = "estimator")
public record EstimatorProperties(BigDecimal gpuCount, BigDecimal opsPerSecondPerGpu) {

    public EstimatorProperties {
        if (gpuCount == null || gpuCount.signum() <= 0) {
            throw new IllegalArgumentException("estimator.gpu-count must be > 0");
        }
        if (opsPerSecondPerGpu == null || opsPerSecondPerGpu.signum() <= 0) {
            throw new IllegalArgumentException("estimator.ops-per-second-per-gpu must be > 0");
        }
    }
}
