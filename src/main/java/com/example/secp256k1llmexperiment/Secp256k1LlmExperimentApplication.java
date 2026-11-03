package com.example.secp256k1llmexperiment;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Secp256k1LlmExperimentApplication {

    static final String API_KEY = "OPENROUTER_API_KEY";

    /** Local, git-ignored file; Spring imports it too (spring.config.import in application.yml). */
    static final Path DOT_ENV = Path.of(".env");

    public static void main(String[] args) {
        // The LLM experiments need a key; weak-entropy is pure local computation and needs none.
        if (requiresApiKey(experimentArg(args)) && !hasApiKey()) {
            System.err.println("""
                    OPENROUTER_API_KEY is not set.

                    Get a key at https://openrouter.ai/keys, then either:
                      - put it into the .env file in the project root (see .env.example):
                            OPENROUTER_API_KEY=sk-or-...
                      - or export it in the shell:
                            export OPENROUTER_API_KEY=sk-or-...
                    and run: ./mvnw spring-boot:run
                    """);
            System.exit(1);
        }
        SpringApplication.run(Secp256k1LlmExperimentApplication.class, args);
    }

    /** Which experiment was selected: {@code --experiment=<name>}, default {@code recovery}. */
    public static String experimentArg(String[] args) {
        String value = "recovery";
        for (String a : args) {
            if (a.startsWith("--experiment=")) {
                value = a.substring("--experiment=".length()).trim();
            }
        }
        return value.isEmpty() ? "recovery" : value;
    }

    /** Only the OpenRouter-based experiments require the key. */
    static boolean requiresApiKey(String experiment) {
        return !experiment.equals("weak-entropy");
    }

    /** Environment variable wins; otherwise look into .env. */
    private static boolean hasApiKey() {
        String fromEnv = System.getenv(API_KEY);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return true;
        }
        if (!Files.isRegularFile(DOT_ENV)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(DOT_ENV)) {
            Properties props = new Properties();
            props.load(reader);
            String fromFile = props.getProperty(API_KEY);
            return fromFile != null && !fromFile.isBlank();
        } catch (IOException e) {
            return false;
        }
    }

}
