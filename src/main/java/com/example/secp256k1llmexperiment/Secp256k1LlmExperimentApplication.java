package com.example.secp256k1llmexperiment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Secp256k1LlmExperimentApplication {

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("""
                    OPENROUTER_API_KEY is not set.

                    Get a key at https://openrouter.ai/keys and run:
                        export OPENROUTER_API_KEY=sk-or-...
                        mvn spring-boot:run
                    """);
            System.exit(1);
        }
        SpringApplication.run(Secp256k1LlmExperimentApplication.class, args);
    }

}
