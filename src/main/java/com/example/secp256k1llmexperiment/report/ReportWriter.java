package com.example.secp256k1llmexperiment.report;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.secp256k1llmexperiment.config.ExperimentProperties;
import com.example.secp256k1llmexperiment.crypto.GeneratedKeyPair;
import com.example.secp256k1llmexperiment.crypto.Secp256k1;
import com.example.secp256k1llmexperiment.estimate.BruteForceEstimator;
import com.example.secp256k1llmexperiment.estimate.BruteForceEstimator.Estimate;
import com.example.secp256k1llmexperiment.llm.PromptType;
import com.example.secp256k1llmexperiment.llm.ResultCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/**
 * Console output plus results/&lt;timestamp&gt;/results.json and summary.csv.
 * The private key is only ever printed to the console, at the very end.
 */
@Component
public class ReportWriter {

    private static final int PREVIEW_LENGTH = 80;
    private static final String LINE = "─".repeat(150);
    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ExperimentProperties properties;
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final PrintStream out = System.out;

    public ReportWriter(ExperimentProperties properties) {
        this.properties = properties;
    }

    // ---------------------------------------------------------------- console

    public void printKeyPair(GeneratedKeyPair keyPair) {
        out.println();
        out.println("1. KEY PAIR (one-time, generated just now, never held any funds)");
        out.println(LINE);
        out.printf(Locale.ROOT, "Public key (compressed, 33 bytes): %s%n", keyPair.publicKeyHex());
        out.printf(Locale.ROOT, "Private key → public key (k·G) took: %.3f ms%n", keyPair.publicKeyMillis());
        out.println("Private key: kept locally, shown at the end of the run.");
    }

    public void printEstimate(Estimate e) {
        out.println();
        out.println("2. BRUTE FORCE ESTIMATE (Pollard's rho, ~√n operations)");
        out.println(LINE);
        out.printf(Locale.ROOT, "Operations needed:            2^128 ≈ %s%n", sci(new BigDecimal(e.operations())));
        out.println("Assumptions (estimator.* in application.yml):");
        out.printf(Locale.ROOT, "  EC operations / s per GPU:  %s%n", sci(e.opsPerSecondPerGpu()));
        out.printf(Locale.ROOT, "  GPUs in the world:          %s%n", sci(e.gpuCount()));
        out.printf(Locale.ROOT, "One GPU:                      %s years%n", sci(e.yearsOneGpu()));
        out.printf(Locale.ROOT, "All GPUs in the world:        %s years%n", sci(e.yearsAllGpus()));
        out.printf(Locale.ROOT, "Age of the universe:          %s years%n", sci(BruteForceEstimator.AGE_OF_UNIVERSE_YEARS));
        out.printf(Locale.ROOT, "All GPUs need                 %.1f × the age of the universe%n", e.allGpusInUniverseAges());
    }

    public void printExperimentStart(int totalCalls) {
        out.println();
        out.printf(Locale.ROOT, "3. ASKING LLMs (%d calls: %d models × %d prompts × %d runs, temperature %.2f, max %d tokens)%n",
                totalCalls, properties.models().size(), PromptType.values().length,
                properties.runsPerPrompt(), properties.temperature(), properties.maxTokens());
        out.println("   Only the PUBLIC key is sent.");
        out.println(LINE);
    }

    public void printProgress(int index, int total, RunResult r) {
        out.printf(Locale.ROOT, "[%2d/%d] %-40s %-6s #%d → %-14s (%d ms)%n",
                index, total, r.model(), r.prompt(), r.run(), r.category(), r.latencyMs());
    }

    public void printResultsTable(List<RunResult> results) {
        out.println();
        out.println("4. RESULTS");
        out.println(LINE);
        out.printf(Locale.ROOT, "%-40s %-6s %-3s %-14s %s%n", "MODEL", "PROMPT", "RUN", "CLASS", "RESPONSE (first " + PREVIEW_LENGTH + " chars)");
        out.println(LINE);
        for (RunResult r : results) {
            out.printf(Locale.ROOT, "%-40s %-6s %-3d %-14s %s%n", r.model(), r.prompt(), r.run(), r.category(), preview(r));
        }
    }

    public void printSummary(List<RunResult> results) {
        out.println();
        out.println("5. SUMMARY BY MODEL");
        out.println(LINE);
        StringBuilder header = new StringBuilder(String.format("%-40s", "MODEL"));
        for (ResultCategory c : ResultCategory.values()) {
            header.append(String.format(" %15s", c));
        }
        out.println(header);
        out.println(LINE);
        Map<String, List<RunResult>> byModel = results.stream()
                .collect(Collectors.groupingBy(RunResult::model, LinkedHashMap::new, Collectors.toList()));
        byModel.forEach((model, rows) -> {
            Map<ResultCategory, Long> counts = count(rows);
            StringBuilder line = new StringBuilder(String.format("%-40s", model));
            for (ResultCategory c : ResultCategory.values()) {
                line.append(String.format(" %15d", counts.get(c)));
            }
            out.println(line);
        });
        long matches = results.stream().filter(r -> r.category() == ResultCategory.MATCH).count();
        out.println(LINE);
        out.printf(Locale.ROOT, "Correct keys: %d of %d answers.%n", matches, results.size());
    }

    public void printFilesWritten(Path dir) {
        out.println();
        out.printf(Locale.ROOT, "Results written to %s (results.json, summary.csv). The private key is NOT in these files.%n",
                dir.toAbsolutePath());
    }

    public void printPrivateKeyReveal(GeneratedKeyPair keyPair, double verificationMillis, boolean verified) {
        out.println();
        out.println("6. THE REAL PRIVATE KEY (never sent anywhere, not saved to files)");
        out.println(LINE);
        out.printf(Locale.ROOT, "Private key: %s%n", Secp256k1.toHex64(keyPair.privateKey()));
        out.printf(Locale.ROOT, "Check: privateKey·G == public key → %s (%.3f ms)%n", verified, verificationMillis);
        out.println("This key is single-use. Do not send funds to it.");
        out.println();
    }

    // ---------------------------------------------------------------- files

    public Path writeFiles(ExperimentReport report, LocalDateTime startedAt) throws IOException {
        Path dir = Path.of(properties.resultsDir(), startedAt.format(DIR_FORMAT));
        Files.createDirectories(dir);
        json.writeValue(dir.resolve("results.json").toFile(), report);
        Files.writeString(dir.resolve("summary.csv"), summaryCsv(report.runs()));
        return dir;
    }

    /** One row per model × prompt: how many answers fell into each class. */
    String summaryCsv(List<RunResult> results) {
        StringBuilder csv = new StringBuilder("model,prompt,runs");
        for (ResultCategory c : ResultCategory.values()) {
            csv.append(',').append(c);
        }
        csv.append('\n');

        Map<String, List<RunResult>> groups = results.stream().collect(Collectors.groupingBy(
                r -> r.model() + "\u0000" + r.prompt(), LinkedHashMap::new, Collectors.toList()));
        groups.values().forEach(rows -> {
            Map<ResultCategory, Long> counts = count(rows);
            csv.append(csvField(rows.getFirst().model())).append(',')
                    .append(rows.getFirst().prompt()).append(',')
                    .append(rows.size());
            for (ResultCategory c : ResultCategory.values()) {
                csv.append(',').append(counts.get(c));
            }
            csv.append('\n');
        });
        return csv.toString();
    }

    // ---------------------------------------------------------------- helpers

    private static Map<ResultCategory, Long> count(List<RunResult> rows) {
        Map<ResultCategory, Long> counts = new EnumMap<>(ResultCategory.class);
        for (ResultCategory c : ResultCategory.values()) {
            counts.put(c, 0L);
        }
        rows.forEach(r -> counts.merge(r.category(), 1L, Long::sum));
        return counts;
    }

    private static String preview(RunResult r) {
        String text = r.error() != null ? r.error() : r.rawResponse();
        if (text == null || text.isBlank()) {
            return r.finishReason() != null ? "<empty, finish_reason=" + r.finishReason() + ">" : "<empty>";
        }
        String oneLine = text.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH) + "…";
    }

    private static String csvField(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    static String sci(BigDecimal value) {
        return String.format(Locale.ROOT, "%.3e", value);
    }
}
