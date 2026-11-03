package com.example.secp256k1llmexperiment.llm;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.example.secp256k1llmexperiment.config.ExperimentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

/**
 * Asks one model (via OpenRouter) to "recover" a private key. Never throws: a failed or
 * timed-out call comes back as {@link LlmResponse#failure}, so one bad model does not stop the experiment.
 */
@Component
public class LlmRecoveryClient {

    private static final Logger log = LoggerFactory.getLogger(LlmRecoveryClient.class);

    private final ChatClient chatClient;
    private final ExperimentProperties properties;

    public LlmRecoveryClient(ChatClient.Builder chatClientBuilder, ExperimentProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    public LlmResponse ask(String model, String prompt) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(properties.temperature())
                .maxTokens(properties.maxTokens());
        Integer reasoningBudget = properties.reasoningMaxTokens().get(model);
        if (reasoningBudget != null) {
            // OpenRouter-specific field, sent as a top-level "reasoning" object
            options.extraBody(Map.of("reasoning", Map.of("max_tokens", reasoningBudget)));
        }

        long start = System.currentTimeMillis();
        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .options(options.build())
                    .call()
                    .chatResponse();
            long latency = System.currentTimeMillis() - start;

            if (response == null || response.getResult() == null) {
                return new LlmResponse("", null, null, latency, null);
            }
            Generation generation = response.getResult();
            String text = generation.getOutput().getText();
            String finishReason = generation.getMetadata().getFinishReason();
            Usage usage = response.getMetadata().getUsage();
            Integer tokens = usage != null ? usage.getTotalTokens() : null;
            return new LlmResponse(text == null ? "" : text, finishReason, tokens, latency, null);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            String error = describe(e, latency);
            log.warn("Call to {} failed: {}", model, error);
            return LlmResponse.failure(error, latency);
        }
    }

    /**
     * Spring wraps a read timeout into "Error while extracting response…", which says nothing.
     * Report the root cause instead, and call a timeout a timeout.
     */
    static String describe(Exception e, long latencyMs) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException || t instanceof SocketTimeoutException
                    || t.getClass().getSimpleName().contains("Timeout")) {
                return "TIMEOUT: no answer within " + latencyMs + " ms (" + t.getClass().getSimpleName() + ")";
            }
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message != null ? ": " + message : "");
    }
}
