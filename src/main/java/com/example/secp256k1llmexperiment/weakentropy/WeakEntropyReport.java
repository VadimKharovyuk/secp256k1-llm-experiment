package com.example.secp256k1llmexperiment.weakentropy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Everything written to results/weak-entropy/&lt;timestamp&gt;/results.json.
 * <p>
 * The "found" private keys ARE included: they are deliberately weak, single-use keys generated in
 * this run with no funds. Showing them is the whole point of the demonstration.
 */
public record WeakEntropyReport(
        String startedAt,
        String finishedAt,
        Environment environment,
        Speed speed,
        List<ScenarioResult> scenarios,
        Extrapolation32 extrapolation32) {

    /**
     * @param cores   logical cores used for the parallel search
     * @param cpu     CPU model if detectable, else null
     * @param jvm     Java version
     * @param osArch  os.arch (e.g. aarch64, x86_64)
     */
    public record Environment(int cores, String cpu, String jvm, String osArch) {
    }

    /**
     * @param warmupCandidates          derivations before timing
     * @param benchmarkCandidates       derivations timed
     * @param candidatesPerSecondPerThread measured single-thread rate
     * @param cores                     logical cores
     * @param candidatesPerSecondAllCores perThread × cores (rough all-core rate)
     */
    public record Speed(
            long warmupCandidates,
            long benchmarkCandidates,
            double candidatesPerSecondPerThread,
            int cores,
            double candidatesPerSecondAllCores) {
    }

    /**
     * One scenario outcome.
     *
     * @param scenario        scenario name
     * @param description     human-readable description
     * @param publicKeyHex    the target public key (compressed hex)
     * @param keyspace        number of possible seeds/keys
     * @param searched        whether the key was actually brute-forced
     * @param found           whether the search found it
     * @param foundSeed       matching seed, or null
     * @param foundPrivateKey matching private key (weak, single-use), or null
     * @param candidateOrdinal 1-based position of the seed within the window, or null
     * @param seconds         search time, or null if not searched
     * @param note            extra context (extrapolation, reference, …)
     */
    public record ScenarioResult(
            String scenario,
            String description,
            String publicKeyHex,
            BigInteger keyspace,
            boolean searched,
            boolean found,
            Long foundSeed,
            String foundPrivateKey,
            Long candidateOrdinal,
            Double seconds,
            String note) {
    }

    /**
     * Extrapolation of a full 2^32 search.
     *
     * @param keyspace                   2^32
     * @param secondsThisMachineAllCores time on this machine at the measured all-core rate
     * @param gpuCount                   assumed GPUs (config)
     * @param candidatesPerSecondPerGpu  assumed per-GPU rate (config)
     * @param secondsOnGpus              time on that many GPUs
     */
    public record Extrapolation32(
            BigInteger keyspace,
            double secondsThisMachineAllCores,
            BigDecimal gpuCount,
            BigDecimal candidatesPerSecondPerGpu,
            double secondsOnGpus) {
    }
}
