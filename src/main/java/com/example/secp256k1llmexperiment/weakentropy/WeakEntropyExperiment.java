package com.example.secp256k1llmexperiment.weakentropy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.secp256k1llmexperiment.config.WeakEntropyProperties;
import com.example.secp256k1llmexperiment.core.Experiment;
import com.example.secp256k1llmexperiment.core.ResultsStore;
import com.example.secp256k1llmexperiment.crypto.GeneratedKeyPair;
import com.example.secp256k1llmexperiment.crypto.KeyPairGenerator;
import com.example.secp256k1llmexperiment.crypto.Secp256k1;
import com.example.secp256k1llmexperiment.weakentropy.WeakEntropyReport.Environment;
import com.example.secp256k1llmexperiment.weakentropy.WeakEntropyReport.Extrapolation32;
import com.example.secp256k1llmexperiment.weakentropy.WeakEntropyReport.ScenarioResult;
import com.example.secp256k1llmexperiment.weakentropy.WeakEntropyReport.Speed;
import com.example.secp256k1llmexperiment.weakentropy.WeakKeyAttacker.Result;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Component;

/**
 * Experiment "weak-entropy": shows the contrast between a SecureRandom key (~2^128 work) and keys
 * from predictable sources, which are found by brute force on a laptop.
 * <p>
 * Every target key is generated inside this run; nothing external is attacked.
 */
@Component
public class WeakEntropyExperiment implements Experiment {

    private static final String LINE = "─".repeat(110);
    private static final BigInteger TWO_32 = BigInteger.ONE.shiftLeft(32);
    private static final MathContext MC = MathContext.DECIMAL64;

    private final WeakKeyGenerator generator;
    private final WeakKeyAttacker attacker;
    private final WeakEntropyProperties properties;
    private final KeyPairGenerator strongGenerator;
    private final ResultsStore results;
    private final SecureRandom secureRandom = new SecureRandom();

    public WeakEntropyExperiment(WeakKeyGenerator generator, WeakKeyAttacker attacker,
                                 WeakEntropyProperties properties, KeyPairGenerator strongGenerator,
                                 ResultsStore results) {
        this.generator = generator;
        this.attacker = attacker;
        this.properties = properties;
        this.strongGenerator = strongGenerator;
        this.results = results;
    }

    @Override
    public String name() {
        return "weak-entropy";
    }

    @Override
    public void run() throws Exception {
        LocalDateTime startedAt = LocalDateTime.now();
        int cores = Runtime.getRuntime().availableProcessors();

        Environment env = new Environment(cores, detectCpu(),
                System.getProperty("java.version"), System.getProperty("os.arch"));
        printEnvironment(env);

        double perThread = attacker.benchmarkPerThread(properties.warmupCandidates(), properties.benchmarkCandidates());
        Speed speed = new Speed(properties.warmupCandidates(), properties.benchmarkCandidates(),
                perThread, cores, perThread * cores);
        printSpeed(speed);

        System.out.println();
        System.out.println("3. SCENARIOS (every target key is generated in this run; the attacker knows only");
        System.out.println("   the public key and the window below)");
        System.out.println(LINE);

        List<ScenarioResult> scenarioResults = new ArrayList<>();
        for (String scenarioName : properties.scenarios()) {
            WeakEntropyScenario scenario = WeakEntropyScenario.valueOf(scenarioName.trim());
            ScenarioResult result = runScenario(scenario, speed);
            scenarioResults.add(result);
            printScenario(result);
        }

        Extrapolation32 extrapolation = extrapolate32(speed);
        printExtrapolation(extrapolation);

        printContrastTable(scenarioResults);

        WeakEntropyReport report = new WeakEntropyReport(
                startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                env, speed, scenarioResults, extrapolation);
        Path dir = results.newRunDir("weak-entropy", startedAt);
        results.writeJson(dir, "results.json", report);
        results.writeString(dir, "summary.csv", contrastCsv(scenarioResults));

        System.out.println();
        System.out.printf(Locale.ROOT, "Results written to %s (results.json, summary.csv).%n", dir.toAbsolutePath());
        System.out.println("The recovered keys are deliberately weak, single-use keys with no funds. Do not reuse them.");
        System.out.println();
    }

    // ---------------------------------------------------------------- scenarios

    private ScenarioResult runScenario(WeakEntropyScenario scenario, Speed speed) {
        return switch (scenario) {
            case SECONDS_DAY -> timeWindowScenario(scenario, truncate(Instant.now(), Duration.ofDays(1)).getEpochSecond(),
                    properties.secondsWindow());
            case MILLIS_HOUR -> timeWindowScenario(scenario, truncate(Instant.now(), Duration.ofHours(1)).toEpochMilli(),
                    properties.millisWindow());
            case SEED_32BIT -> seed32Scenario(scenario, speed);
            case SECURE_RANDOM -> secureRandomScenario(scenario);
        };
    }

    /** SECONDS_DAY / MILLIS_HOUR: pick a target seed inside the window, then actually search it. */
    private ScenarioResult timeWindowScenario(WeakEntropyScenario scenario, long windowStart, long windowSize) {
        long offset = Math.floorMod(secureRandom.nextLong(), windowSize);
        long targetSeed = windowStart + offset;
        ECPoint target = generator.publicKey(targetSeed);

        Result r = attacker.search(target, windowStart, windowSize);
        String privateKey = r.found() ? Secp256k1.toHex64(generator.privateKey(r.foundSeed())) : null;
        Long ordinal = r.found() ? r.foundSeed() - windowStart + 1 : null;
        String note = String.format(Locale.ROOT, "Window of %,d seeds searched on %d cores",
                windowSize, Runtime.getRuntime().availableProcessors());

        return new ScenarioResult(scenario.name(), scenario.description(), Secp256k1.toCompressedHex(target),
                BigInteger.valueOf(windowSize), true, r.found(), r.found() ? r.foundSeed() : null,
                privateKey, ordinal, r.seconds(), note);
    }

    /** SEED_32BIT: generate a target but do NOT enumerate 2^32; report an extrapolation instead. */
    private ScenarioResult seed32Scenario(WeakEntropyScenario scenario, Speed speed) {
        long targetSeed = secureRandom.nextInt() & 0xFFFFFFFFL;
        ECPoint target = generator.publicKey(targetSeed);
        double seconds = TWO_32.doubleValue() / speed.candidatesPerSecondAllCores();
        String note = "Not enumerated. Full 2^32 search on this machine (all cores) ≈ " + humanTime(seconds);

        return new ScenarioResult(scenario.name(), scenario.description(), Secp256k1.toCompressedHex(target),
                TWO_32, false, false, null, null, null, null, note);
    }

    /** SECURE_RANDOM: a proper key; reference the ~2^128 figure from the recovery experiment. */
    private ScenarioResult secureRandomScenario(WeakEntropyScenario scenario) {
        GeneratedKeyPair keyPair = strongGenerator.generate();
        String note = "~2^128 operations (Pollard's rho). See the recovery experiment: "
                + "far beyond all GPUs in the world running for the age of the universe.";

        return new ScenarioResult(scenario.name(), scenario.description(), keyPair.publicKeyHex(),
                Secp256k1.N, false, false, null, null, null, null, note);
    }

    private Extrapolation32 extrapolate32(Speed speed) {
        double secondsThisMachine = TWO_32.doubleValue() / speed.candidatesPerSecondAllCores();
        BigDecimal gpuCount = properties.extrapolation().gpuCount();
        BigDecimal perGpu = properties.extrapolation().candidatesPerSecondPerGpu();
        double secondsOnGpus = new BigDecimal(TWO_32)
                .divide(gpuCount.multiply(perGpu), MC).doubleValue();
        return new Extrapolation32(TWO_32, secondsThisMachine, gpuCount, perGpu, secondsOnGpus);
    }

    // ---------------------------------------------------------------- console

    private void printEnvironment(Environment env) {
        System.out.println();
        System.out.println("1. ENVIRONMENT");
        System.out.println(LINE);
        System.out.printf(Locale.ROOT, "Logical cores: %d%n", env.cores());
        System.out.printf(Locale.ROOT, "CPU:           %s%n", env.cpu() == null ? "<unknown>" : env.cpu());
        System.out.printf(Locale.ROOT, "JVM:           %s (%s)%n", env.jvm(), env.osArch());
    }

    private void printSpeed(Speed speed) {
        System.out.println();
        System.out.println("2. SEARCH SPEED (weak key derivation + k·G, after JVM warm-up)");
        System.out.println(LINE);
        System.out.printf(Locale.ROOT, "Warm-up candidates:        %,d%n", speed.warmupCandidates());
        System.out.printf(Locale.ROOT, "Benchmark candidates:      %,d%n", speed.benchmarkCandidates());
        System.out.printf(Locale.ROOT, "Candidates/s, one thread:  %,.0f%n", speed.candidatesPerSecondPerThread());
        System.out.printf(Locale.ROOT, "Candidates/s, %d cores:     %,.0f%n", speed.cores(), speed.candidatesPerSecondAllCores());
    }

    private void printScenario(ScenarioResult r) {
        System.out.println();
        System.out.printf(Locale.ROOT, "%s — %s%n", r.scenario(), r.description());
        System.out.printf(Locale.ROOT, "  Public key: %s%n", r.publicKeyHex());
        System.out.printf(Locale.ROOT, "  Keyspace:   %s%n", keyspaceLabel(r.keyspace()));
        if (r.searched()) {
            if (r.found()) {
                System.out.printf(Locale.ROOT, "  FOUND in %.2f s — seed %d, candidate #%,d of %,d%n",
                        r.seconds(), r.foundSeed(), r.candidateOrdinal(), r.keyspace());
                System.out.printf(Locale.ROOT, "  Private key (weak, single-use): %s%n", r.foundPrivateKey());
            } else {
                System.out.printf(Locale.ROOT, "  NOT found in the %,d-seed window (%.2f s)%n", r.keyspace(), r.seconds());
            }
        } else {
            System.out.printf(Locale.ROOT, "  Not searched. %s%n", r.note());
        }
    }

    private void printExtrapolation(Extrapolation32 e) {
        System.out.println();
        System.out.println("4. EXTRAPOLATION: full 2^32 search (SEED_32BIT)");
        System.out.println(LINE);
        System.out.printf(Locale.ROOT, "This machine (all cores): %s%n", humanTime(e.secondsThisMachineAllCores()));
        System.out.printf(Locale.ROOT, "Assumptions (weak-entropy.extrapolation.* in application.yml):%n");
        System.out.printf(Locale.ROOT, "  GPUs:                   %s%n", sci(e.gpuCount()));
        System.out.printf(Locale.ROOT, "  Candidates/s per GPU:   %s%n", sci(e.candidatesPerSecondPerGpu()));
        System.out.printf(Locale.ROOT, "On %s GPUs:              %s%n", sci(e.gpuCount()), humanTime(e.secondsOnGpus()));
    }

    private void printContrastTable(List<ScenarioResult> rows) {
        System.out.println();
        System.out.println("5. CONTRAST");
        System.out.println(LINE);
        System.out.printf(Locale.ROOT, "%-15s %-22s %-10s %s%n", "SCENARIO", "KEYSPACE", "SEARCHED", "RESULT");
        System.out.println(LINE);
        for (ScenarioResult r : rows) {
            String result;
            if (r.searched()) {
                result = r.found()
                        ? String.format(Locale.ROOT, "found in %.2f s", r.seconds())
                        : "not found in window";
            } else {
                result = r.note();
            }
            System.out.printf(Locale.ROOT, "%-15s %-22s %-10s %s%n",
                    r.scenario(), keyspaceLabel(r.keyspace()), r.searched() ? "yes" : "no", result);
        }
    }

    // ---------------------------------------------------------------- files

    private String contrastCsv(List<ScenarioResult> rows) {
        StringBuilder csv = new StringBuilder("scenario,keyspace,searched,found,seconds,found_seed,candidate_ordinal\n");
        for (ScenarioResult r : rows) {
            csv.append(r.scenario()).append(',')
                    .append(r.keyspace()).append(',')
                    .append(r.searched()).append(',')
                    .append(r.found()).append(',')
                    .append(r.seconds() == null ? "" : "%.3f".formatted(r.seconds())).append(',')
                    .append(r.foundSeed() == null ? "" : r.foundSeed()).append(',')
                    .append(r.candidateOrdinal() == null ? "" : r.candidateOrdinal()).append('\n');
        }
        return csv.toString();
    }

    // ---------------------------------------------------------------- helpers

    private static Instant truncate(Instant instant, Duration unit) {
        long seconds = instant.getEpochSecond();
        long unitSeconds = unit.getSeconds();
        return Instant.ofEpochSecond(seconds - Math.floorMod(seconds, unitSeconds));
    }

    private static String keyspaceLabel(BigInteger keyspace) {
        if (keyspace.equals(TWO_32)) {
            return "2^32 (~4.3e9)";
        }
        if (keyspace.equals(Secp256k1.N)) {
            return "~2^256";
        }
        return String.format(Locale.ROOT, "%,d", keyspace.longValueExact());
    }

    private static String humanTime(double seconds) {
        if (seconds < 1) {
            return String.format(Locale.ROOT, "%.3f s", seconds);
        }
        if (seconds < 60) {
            return String.format(Locale.ROOT, "%.1f s", seconds);
        }
        if (seconds < 3600) {
            return String.format(Locale.ROOT, "%.1f min", seconds / 60);
        }
        if (seconds < 86400) {
            return String.format(Locale.ROOT, "%.1f hours", seconds / 3600);
        }
        if (seconds < 86400 * 365.25) {
            return String.format(Locale.ROOT, "%.1f days", seconds / 86400);
        }
        return String.format(Locale.ROOT, "%.2e years", seconds / (86400 * 365.25));
    }

    private static String sci(BigDecimal value) {
        return String.format(Locale.ROOT, "%.2e", value);
    }

    /** Best-effort CPU model; null if it cannot be read. */
    private static String detectCpu() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb;
            if (os.contains("mac")) {
                pb = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string");
            } else if (os.contains("linux")) {
                pb = new ProcessBuilder("sh", "-c", "grep -m1 'model name' /proc/cpuinfo | cut -d: -f2");
            } else {
                return null;
            }
            Process p = pb.redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return out.isBlank() ? null : out;
        } catch (Exception e) {
            return null;
        }
    }
}
