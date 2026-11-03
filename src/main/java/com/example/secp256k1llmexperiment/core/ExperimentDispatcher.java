package com.example.secp256k1llmexperiment.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.secp256k1llmexperiment.Secp256k1LlmExperimentApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * The single {@link CommandLineRunner} of the app. Picks the experiment named by
 * {@code --experiment=<name>} (default {@code recovery}) and runs only that one.
 * <p>
 * It prints nothing of its own, so {@code recovery} produces exactly the same output as before
 * this dispatcher existed.
 */
@Component
public class ExperimentDispatcher implements CommandLineRunner {

    private final Map<String, Experiment> byName = new LinkedHashMap<>();

    public ExperimentDispatcher(List<Experiment> experiments) {
        for (Experiment e : experiments) {
            byName.put(e.name(), e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        String name = Secp256k1LlmExperimentApplication.experimentArg(args);
        Experiment experiment = byName.get(name);
        if (experiment == null) {
            System.err.printf("Unknown experiment '%s'. Available: %s%n", name, String.join(", ", byName.keySet()));
            System.exit(2);
            return;
        }
        experiment.run();
    }
}
