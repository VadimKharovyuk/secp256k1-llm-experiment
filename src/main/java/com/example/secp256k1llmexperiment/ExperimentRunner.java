package com.example.secp256k1llmexperiment;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.example.secp256k1llmexperiment.config.ExperimentProperties;
import com.example.secp256k1llmexperiment.crypto.GeneratedKeyPair;
import com.example.secp256k1llmexperiment.crypto.KeyPairGenerator;
import com.example.secp256k1llmexperiment.crypto.KeyVerifier;
import com.example.secp256k1llmexperiment.crypto.KeyVerifier.Verification;
import com.example.secp256k1llmexperiment.estimate.BruteForceEstimator;
import com.example.secp256k1llmexperiment.estimate.BruteForceEstimator.Estimate;
import com.example.secp256k1llmexperiment.llm.LlmRecoveryClient;
import com.example.secp256k1llmexperiment.llm.LlmResponse;
import com.example.secp256k1llmexperiment.llm.PromptFactory;
import com.example.secp256k1llmexperiment.llm.PromptType;
import com.example.secp256k1llmexperiment.llm.ResponseClassifier;
import com.example.secp256k1llmexperiment.llm.ResponseClassifier.Classification;
import com.example.secp256k1llmexperiment.report.ExperimentReport;
import com.example.secp256k1llmexperiment.report.ReportWriter;
import com.example.secp256k1llmexperiment.report.RunResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Key generation → brute-force estimate → LLM runs → classification and verification → report.
 */
@Component
public class ExperimentRunner implements CommandLineRunner {

    private final ExperimentProperties properties;
    private final KeyPairGenerator keyPairGenerator;
    private final BruteForceEstimator estimator;
    private final PromptFactory promptFactory;
    private final LlmRecoveryClient llm;
    private final ResponseClassifier classifier;
    private final KeyVerifier verifier;
    private final ReportWriter report;

    public ExperimentRunner(ExperimentProperties properties, KeyPairGenerator keyPairGenerator,
                            BruteForceEstimator estimator, PromptFactory promptFactory, LlmRecoveryClient llm,
                            ResponseClassifier classifier, KeyVerifier verifier, ReportWriter report) {
        this.properties = properties;
        this.keyPairGenerator = keyPairGenerator;
        this.estimator = estimator;
        this.promptFactory = promptFactory;
        this.llm = llm;
        this.classifier = classifier;
        this.verifier = verifier;
        this.report = report;
    }

    @Override
    public void run(String... args) throws Exception {
        LocalDateTime startedAt = LocalDateTime.now();

        GeneratedKeyPair keyPair = keyPairGenerator.generate();
        report.printKeyPair(keyPair);

        Estimate estimate = estimator.estimate();
        report.printEstimate(estimate);

        Map<PromptType, String> prompts = new EnumMap<>(PromptType.class);
        for (PromptType type : PromptType.values()) {
            prompts.put(type, promptFactory.build(type, keyPair.publicKeyHex()));
        }

        int total = properties.models().size() * prompts.size() * properties.runsPerPrompt();
        report.printExperimentStart(total);

        List<RunResult> results = new ArrayList<>(total);
        for (String model : properties.models()) {
            for (PromptType type : PromptType.values()) {
                for (int run = 1; run <= properties.runsPerPrompt(); run++) {
                    LlmResponse response = llm.ask(model, prompts.get(type));
                    Classification c = classifier.classify(response, keyPair.publicKey());
                    RunResult result = new RunResult(
                            model, type, run, c.category(), c.candidate(),
                            c.verification() != null ? c.verification().inRange() : null,
                            c.verification() != null ? c.verification().millis() : null,
                            response.text(), response.finishReason(), response.totalTokens(),
                            response.latencyMs(), response.error());
                    results.add(result);
                    report.printProgress(results.size(), total, result);
                }
            }
        }

        report.printResultsTable(results);
        report.printSummary(results);

        // The same check every candidate went through, now with the real key
        Verification real = verifier.verify(keyPair.privateKey(), keyPair.publicKey());

        ExperimentReport experimentReport = new ExperimentReport(
                startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                keyPair.publicKeyHex(),
                keyPair.publicKeyMillis(),
                real.millis(),
                properties.temperature(),
                properties.maxTokens(),
                properties.reasoningMaxTokens(),
                properties.runsPerPrompt(),
                properties.models(),
                prompts,
                estimate,
                results);
        Path dir = report.writeFiles(experimentReport, startedAt);
        report.printFilesWritten(dir);

        report.printPrivateKeyReveal(keyPair, real.millis(), real.matches());
    }
}
