package com.example.secp256k1llmexperiment.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.example.secp256k1llmexperiment.config.ExperimentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/**
 * Shared writer for the new experiments: creates {@code results/<experiment>/<timestamp>/}
 * and writes JSON and text files into it. The recovery experiment keeps its own
 * {@code results/<timestamp>/} layout untouched.
 */
@Component
public class ResultsStore {

    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final String baseDir;
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public ResultsStore(ExperimentProperties properties) {
        this.baseDir = properties.resultsDir();
    }

    /** {@code results/<experiment>/<timestamp>/}, created on disk. */
    public Path newRunDir(String experiment, LocalDateTime startedAt) throws IOException {
        Path dir = Path.of(baseDir, experiment, startedAt.format(DIR_FORMAT));
        Files.createDirectories(dir);
        return dir;
    }

    public void writeJson(Path dir, String fileName, Object data) throws IOException {
        json.writeValue(dir.resolve(fileName).toFile(), data);
    }

    public void writeString(Path dir, String fileName, String content) throws IOException {
        Files.writeString(dir.resolve(fileName), content);
    }
}
