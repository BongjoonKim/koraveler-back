package server.nadeliv.i18n.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AWS Bedrock (Claude)를 사용한 블로그 글 번역 클라이언트.
 * HTML 태그를 플레이스홀더로 보호하여 번역 시 태그 손상 방지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockPostTranslationClient {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cloud.aws.bedrock.model-id}")
    private String modelId;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private static final String SYSTEM_PROMPT = """
            You are a professional travel blog translator.
            Translate the following travel blog content from %s to %s.

            RULES:
            1. Maintain the casual, personal tone of a travel blogger.
            2. The content contains [[PH_N]] placeholders (e.g., [[PH_1]], [[PH_2]]) representing HTML tags. Preserve ALL placeholders exactly as-is. Do not modify, translate, remove, or reorder them.
            3. Only translate the text between placeholders.
            4. Keep place names in their commonly used form in the target language.
            5. Do NOT translate proper nouns (hotel names, restaurant names, brand names).
            6. Adapt cultural references and idioms naturally.
            7. Match the paragraph structure of the original.
            8. For prices, keep original amounts but adapt currency notation.

            Respond ONLY in this JSON format without any markdown or code blocks:
            {"title": "translated title", "content": "translated content with [[PH_N]] placeholders preserved", "summary": "translated summary"}
            """;

    public TranslationResult translate(String title, String content, String summary,
                                       String sourceLocale, String targetLocale) {
        try {
            // HTML 태그를 플레이스홀더로 치환하여 보호
            Map<String, String> tagMap = new LinkedHashMap<>();
            String protectedContent = protectHtmlTags(content, tagMap);

            log.info("HTML 태그 보호: {} 개 태그를 플레이스홀더로 치환", tagMap.size());

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
                    protectedContent);

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
                        unescapeHtmlEntities(parsed.getOrDefault("title", title)),
                        restoreHtmlTags(parsed.getOrDefault("content", protectedContent), tagMap),
                        unescapeHtmlEntities(parsed.get("summary")),
                        modelId
                );
            } catch (Exception e) {
                log.warn("Bedrock 응답 JSON 파싱 실패, 전체 응답을 content로 사용: {}", e.getMessage());
                return new TranslationResult(title, restoreHtmlTags(responseText, tagMap), summary, modelId);
            }

        } catch (Exception e) {
            log.error("Bedrock 번역 API 호출 실패 ({}→{}): {}", sourceLocale, targetLocale, e.getMessage());
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    /**
     * HTML 태그를 [[PH_N]] 플레이스홀더로 치환.
     * AI 모델이 HTML 태그 속성(src, href, class 등)을 변형하는 것을 원천 방지.
     */
    private String protectHtmlTags(String html, Map<String, String> tagMap) {
        if (html == null) return null;
        Matcher matcher = HTML_TAG_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        int counter = 0;
        while (matcher.find()) {
            counter++;
            String placeholder = "[[PH_" + counter + "]]";
            tagMap.put(placeholder, matcher.group());
            matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 번역 후 [[PH_N]] 플레이스홀더를 원래 HTML 태그로 복원.
     */
    private String restoreHtmlTags(String text, Map<String, String> tagMap) {
        if (text == null || tagMap.isEmpty()) return text;
        String result = text;
        for (Map.Entry<String, String> entry : tagMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return unescapeHtmlEntities(result);
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

    /**
     * 번역 결과에서 잔여 HTML 엔티티를 원래 문자로 복원 (안전망).
     */
    private String unescapeHtmlEntities(String text) {
        if (text == null) return null;
        return text
                .replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'");
    }

    public record TranslationResult(String title, String content, String summary, String modelUsed) {}
}
