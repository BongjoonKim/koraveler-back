package server.koraveler.i18n.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.Map;

/**
 * AWS Bedrock (Claude)를 사용한 블로그 글 번역 클라이언트.
 * 기존 AwsTranslateConfig의 BedrockRuntimeClient 빈을 주입받아 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockPostTranslationClient {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cloud.aws.bedrock.model-id}")
    private String modelId;

    private static final String SYSTEM_PROMPT = """
            You are a professional travel blog translator.
            Translate the following travel blog content from %s to %s.

            RULES:
            1. Maintain the casual, personal tone of a travel blogger.
            2. Preserve ALL HTML formatting exactly as-is (tags, attributes, classes).
            3. Keep place names in their commonly used form in the target language.
            4. Do NOT translate proper nouns (hotel names, restaurant names, brand names).
            5. Adapt cultural references and idioms naturally.
            6. Preserve all image tags, links, and embedded content unchanged.
            7. Match the paragraph structure of the original.
            8. For prices, keep original amounts but adapt currency notation.

            Respond ONLY in this JSON format without any markdown or code blocks:
            {"title": "translated title", "content": "translated content (preserve HTML)", "summary": "translated summary"}
            """;

    public TranslationResult translate(String title, String content, String summary,
                                       String sourceLocale, String targetLocale) {
        try {
            String systemPrompt = String.format(SYSTEM_PROMPT,
                    getLanguageName(sourceLocale),
                    getLanguageName(targetLocale));

            String userMessage = String.format("""
                    Title: %s

                    Summary: %s

                    Content:
                    %s
                    """, title,
                    summary != null ? summary : "",
                    content);

            ConverseRequest converseRequest = ConverseRequest.builder()
                    .modelId(modelId)
                    .system(SystemContentBlock.builder().text(systemPrompt).build())
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(userMessage))
                            .build())
                    .build();

            ConverseResponse response = bedrockRuntimeClient.converse(converseRequest);
            String responseText = response.output().message().content().get(0).text().trim();

            try {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = objectMapper.readValue(responseText, Map.class);
                return new TranslationResult(
                        parsed.getOrDefault("title", title),
                        parsed.getOrDefault("content", content),
                        parsed.get("summary"),
                        modelId
                );
            } catch (Exception e) {
                log.warn("Bedrock 응답 JSON 파싱 실패, 전체 응답을 content로 사용: {}", e.getMessage());
                return new TranslationResult(title, responseText, summary, modelId);
            }

        } catch (Exception e) {
            log.error("Bedrock 번역 API 호출 실패 ({}→{}): {}", sourceLocale, targetLocale, e.getMessage());
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    private String getLanguageName(String code) {
        return switch (code) {
            case "ko" -> "Korean";
            case "en" -> "English";
            case "zh" -> "Chinese (Simplified)";
            case "ja" -> "Japanese";
            default -> "English";
        };
    }

    public record TranslationResult(String title, String content, String summary, String modelUsed) {}
}
