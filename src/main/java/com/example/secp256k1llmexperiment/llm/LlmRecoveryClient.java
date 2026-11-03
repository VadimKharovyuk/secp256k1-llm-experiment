package com.example.secp256k1llmexperiment.llm;

import com.example.secp256k1llmexperiment.config.ExperimentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;
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
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(properties.temperature())
                .maxTokens(properties.maxTokens())
                .build();

        long start = System.currentTimeMillis();
        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .options(options)
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
            log.warn("Call to {} failed: {}", model, e.toString());
            return LlmResponse.failure(e.getClass().getSimpleName() + ": " + e.getMessage(), latency);
        }
    }
}
