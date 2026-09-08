package com.transcriber.workerservice.service;

import com.transcriber.workerservice.config.AudioProcessingProperties;
import com.transcriber.workerservice.dto.QuestionsExtractionResult;
import com.transcriber.workerservice.exception.GroqApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqApiClient {

    private static final String SYSTEM_PROMPT = """
            Ты помощник, который анализирует текст интервью и извлекает из него список
            заданных технический вопросов  свазанных с програмиированием и работой програмиста. 
            Вопросы по типу Можем остаться с видео, можем без видео, как тебе тоже удобнее будет? не должны попасть в итоговый список
            Верни ТОЛЬКО валидный JSON в формате {"questions": ["вопрос 1", "вопрос 2", ...]},
            без дополнительного текста, markdown или пояснений. Если вопросов в тексте нет, верни {"questions": []}.
            Это может быть фрагмент более длинного текста интервью — извлекай вопросы
            из переданного фрагмента независимо от остального текста.
            """;

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient groqChatClient;
    private final AudioProcessingProperties audioProcessingProperties;

    private final ObjectMapper objectMapper;

    public String transcribe(File compressedAudioFile) {
        try {
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(new FileSystemResource(compressedAudioFile));
            AudioTranscriptionResponse response = transcriptionModel.call(prompt);

            String text = response.getResult().getOutput();
            if (!StringUtils.hasText(text)) {
                throw new GroqApiException(200, "Groq API returned empty transcription");
            }
            return text;
        } catch (GroqApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Groq transcription call failed: file={}, error={}", compressedAudioFile.getName(), ex.getMessage());
            throw new GroqApiException(0, "Failed to call Groq transcription API: " + ex.getMessage(), ex);
        }
    }

    public List<String> extractQuestions(String transcriptionText) {
        return extractQuestionsFromText(transcriptionText);
    }

    private List<String> extractQuestionsFromText(String text) {
        int maxTokens = audioProcessingProperties.getMaxQuestionExtractionTokens();
        int estimatedTokens = estimateTokenCount(text);

        if (estimatedTokens <= maxTokens) {
            return callGroqForQuestions(text);
        }

        int chunkCount = (int) Math.ceil((double) estimatedTokens / maxTokens);
        log.info("Transcription text too long ({} estimated tokens), splitting into {} chunks",
                estimatedTokens, chunkCount);

        List<String> chunks = splitIntoChunks(text, chunkCount);
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            if (!StringUtils.hasText(chunk)) {
                continue;
            }
            if (estimateTokenCount(chunk) > maxTokens) {
                result.addAll(extractQuestionsFromText(chunk));
            } else {
                result.addAll(callGroqForQuestions(chunk));
            }
        }
        return result;
    }

    private List<String> callGroqForQuestions(String text) {
        try {
            ChatResponse chatResponse = groqChatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(text)
                    .call()
                    .chatResponse();

            String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
            String content = chatResponse.getResult().getOutput().getText();

            if (!"stop".equalsIgnoreCase(finishReason)) {
                log.warn("Groq generation did not finish normally: finishReason={}, inputLength={}",
                        finishReason, text.length());
            }

            if (!StringUtils.hasText(content)) {
                throw new GroqApiException(0,
                        "Groq returned empty content, finishReason=" + finishReason);
            }

            content = stripMarkdownJsonFence(content);

            QuestionsExtractionResult result = objectMapper.readValue(content, QuestionsExtractionResult.class);

            if (result.getQuestions() == null) {
                return List.of();
            }
            return result.getQuestions();
        } catch (GroqApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to extract questions via Groq: error={}", ex.getMessage());
            throw new GroqApiException(0, "Failed to extract questions via Groq: " + ex.getMessage(), ex);
        }
    }

    private String stripMarkdownJsonFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    private int estimateTokenCount(String text) {
        return text.length() / 3;
    }

    private List<String> splitIntoChunks(String text, int chunkCount) {
        if (chunkCount <= 1) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>(chunkCount);
        int chunkSize = text.length() / chunkCount;
        int start = 0;

        for (int i = 0; i < chunkCount - 1; i++) {
            int splitIndex = findSplitIndex(text, start + chunkSize);
            if (splitIndex <= start) {
                splitIndex = Math.min(start + chunkSize, text.length());
            }
            chunks.add(text.substring(start, splitIndex).trim());
            start = splitIndex;
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }

        if (start < text.length()) {
            chunks.add(text.substring(start).trim());
        }

        return chunks;
    }

    private int findSplitIndex(String text, int targetIndex) {
        if (targetIndex >= text.length()) {
            return text.length();
        }
        int splitIndex = text.indexOf(' ', targetIndex);
        if (splitIndex == -1) {
            return text.length();
        }
        return splitIndex;
    }
}
