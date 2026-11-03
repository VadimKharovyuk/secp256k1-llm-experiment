package com.example.secp256k1llmexperiment.weakentropy;

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Component;

/**
 * Brute-forces a weakly generated key: for every seed in a window, derives the candidate and
 * compares its public key with the target. Runs on all cores and stops at the first match.
 * <p>
 * Only ever used against a public key generated in the same run — never an externally supplied key.
 */
@Component
public class WeakKeyAttacker {

    private final WeakKeyGenerator generator;

    public WeakKeyAttacker(WeakKeyGenerator generator) {
        this.generator = generator;
    }

    /**
     * @param found           whether a matching seed was found
     * @param foundSeed       the matching seed, or null
     * @param candidatesScanned how many seeds were tried (approximate under early stop)
     * @param seconds         wall-clock search time
     */
    public record Result(boolean found, Long foundSeed, long candidatesScanned, double seconds) {
    }

    /** Searches seeds {@code [startSeed, startSeed + count)} in parallel. */
    public Result search(ECPoint target, long startSeed, long count) {
        AtomicLong scanned = new AtomicLong();
        long t0 = System.nanoTime();
        OptionalLong hit = LongStream.range(0, count).parallel()
                .filter(i -> {
                    scanned.incrementAndGet();
                    return generator.publicKey(startSeed + i).equals(target);
                })
                .map(i -> startSeed + i)
                .findAny();
        double seconds = (System.nanoTime() - t0) / 1_000_000_000.0;
        return new Result(hit.isPresent(), hit.isPresent() ? hit.getAsLong() : null, scanned.get(), seconds);
    }

    /**
     * Measures derivation speed (one candidate = one weak key + one k·G), single-threaded,
     * after a warm-up so the JIT has compiled the hot path.
     *
     * @return candidates per second on one thread
     */
    public double benchmarkPerThread(int warmupCandidates, int benchmarkCandidates) {
        long blackhole = 0;
        for (int i = 0; i < warmupCandidates; i++) {
            blackhole += generator.publicKey(Long.MIN_VALUE + i).hashCode();
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < benchmarkCandidates; i++) {
            blackhole += generator.publicKey(Long.MIN_VALUE + warmupCandidates + i).hashCode();
        }
        double seconds = (System.nanoTime() - t0) / 1_000_000_000.0;
        if (blackhole == 42L) {
            System.out.print(""); // keep the JIT from eliminating the loop above
        }
        return benchmarkCandidates / seconds;
    }
}
