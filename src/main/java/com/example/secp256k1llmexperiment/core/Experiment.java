package com.example.secp256k1llmexperiment.core;

/**
 * One runnable experiment. Selected at startup by {@code --experiment=<name>}
 * (default {@code recovery}) and dispatched by {@link ExperimentDispatcher}.
 */
public interface Experiment {

    /** Value matched against the {@code --experiment=} argument, e.g. {@code recovery}. */
    String name();

    void run() throws Exception;
}
