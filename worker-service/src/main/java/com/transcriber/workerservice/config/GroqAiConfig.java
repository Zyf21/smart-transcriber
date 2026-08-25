package com.transcriber.workerservice.config;

import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqAiConfig {

    @Value("${app.ai.text-provider:groq}")
    private String textProvider;

    @Value("${app.ai.groq.api-key}")
    private String groqApiKey;
    @Value("${app.ai.groq.base-url}")
    private String groqBaseUrl;
    @Value("${app.ai.groq.chat-model}")
    private String groqChatModel;

    @Value("${app.ai.gemini.api-key}")
    private String geminiApiKey;
    @Value("${app.ai.gemini.base-url}")
    private String geminiBaseUrl;
    @Value("${app.ai.gemini.chat-model}")
    private String geminiChatModel;

    @Bean
    public ChatClient groqChatClient() {
        boolean useGemini = "gemini".equalsIgnoreCase(textProvider);

        String apiKey = useGemini ? geminiApiKey : groqApiKey;
        String baseUrl = useGemini ? geminiBaseUrl : groqBaseUrl;
        String model = useGemini ? geminiChatModel : groqChatModel;

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .maxTokens(4096)
                .temperature(0.1)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(options)
                .build();

        return ChatClient.builder(chatModel).build();
    }
}
