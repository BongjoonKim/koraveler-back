package server.nadeliv.i18n.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AWS Bedrock (Claude)를 사용한 블로그 글 번역 클라이언트.
 * - HTML 태그를 플레이스홀더로 보호하여 번역 시 태그 손상 방지.
 * - 긴 글은 청크 분할 번역으로 max output tokens 잘림 방지.
 * - 응답 파싱 실패 시 예외를 던져 Job 재시도/FAILED 처리로 넘긴다.
 *   (잘린 원문 응답을 번역 결과로 저장하면 안 됨 — SEO 오염의 원인)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockPostTranslationClient {

    private final BedrockRuntimeClient bedrockRuntimeClient;

    // 모델이 JSON 문자열 내 제어문자를 이스케이프하지 않는 경우가 있어 관대하게 파싱
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    @Value("${cloud.aws.bedrock.model-id}")
    private String modelId;

    @Value("${nadeliv.i18n.max-output-tokens:8192}")
    private int maxOutputTokens;

    // 청크 분할 기준 (플레이스홀더 치환 후 원문 문자 수).
    // 출력 언어(zh/ja)는 문자당 토큰 수가 높아 보수적으로 설정.
    @Value("${nadeliv.i18n.chunk-size-chars:3000}")
    private int chunkSizeChars;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[\\[PH_\\d+]]");

    private static final String COMMON_RULES = """
            RULES:
            1. Maintain the casual, personal tone of a travel blogger.
            2. The content contains [[PH_N]] placeholders (e.g., [[PH_1]], [[PH_2]]) representing HTML tags. Preserve ALL placeholders exactly as-is. Do not modify, translate, remove, or reorder them.
            3. Only translate the text between placeholders.
            4. Keep place names in their commonly used form in the target language. Korean place names should be transliterated/translated to their standard form in the target language (e.g., 월출산 → Wolchulsan / 月出山).
            5. Do NOT translate proper nouns (hotel names, restaurant names, brand names).
            6. Adapt cultural references and idioms naturally.
            7. Match the paragraph structure of the original.
            8. For prices, keep original amounts but adapt currency notation.
            9. Inside JSON string values, escape double quotes and newlines properly (\\" and \\n).
            """;

    private static final String FULL_SYSTEM_PROMPT = """
            You are a professional travel blog translator.
            Translate the following travel blog content from %s to %s.

            """ + COMMON_RULES + """

            Respond ONLY in this JSON format without any markdown or code blocks:
            {"title": "translated title", "content": "translated content with [[PH_N]] placeholders preserved", "summary": "translated summary"}
            """;

    private static final String CHUNK_SYSTEM_PROMPT = """
            You are a professional travel blog translator.
            Translate the following fragment of a travel blog post from %s to %s.

            """ + COMMON_RULES + """

            Respond ONLY in this JSON format without any markdown or code blocks:
            {"text": "translated fragment with [[PH_N]] placeholders preserved"}
            """;

    private static final String TITLE_SYSTEM_PROMPT = """
            You are a professional travel blog translator.
            Translate the following travel blog title and summary from %s to %s.
            Keep place names in their standard form in the target language.

            Respond ONLY in this JSON format without any markdown or code blocks:
            {"title": "translated title", "summary": "translated summary"}
            """;

    public TranslationResult translate(String title, String content, String summary,
                                       String sourceLocale, String targetLocale) {
        try {
            // HTML 태그를 플레이스홀더로 치환하여 보호
            Map<String, String> tagMap = new LinkedHashMap<>();
            String protectedContent = protectHtmlTags(content, tagMap);

            log.info("HTML 태그 보호: {} 개 태그를 플레이스홀더로 치환, 원문 {}자 → 보호 후 {}자",
                    tagMap.size(), content != null ? content.length() : 0,
                    protectedContent != null ? protectedContent.length() : 0);

            String source = getLanguageName(sourceLocale);
            String target = getLanguageName(targetLocale);

            TranslationResult result;
            if (protectedContent == null || protectedContent.length() <= chunkSizeChars) {
                result = translateSingle(title, protectedContent, summary, source, target, tagMap);
            } else {
                result = translateChunked(title, protectedContent, summary, source, target, tagMap);
            }

            // 최종 검증: 복원되지 않은 플레이스홀더가 남아 있으면 실패 처리
            if (result.content() != null && result.content().contains("[[PH")) {
                throw new IllegalStateException("Unrestored placeholders remain in translated content");
            }
            return result;

        } catch (Exception e) {
            log.error("Bedrock 번역 실패 ({}→{}): {}", sourceLocale, targetLocale, e.getMessage());
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    /** 짧은 글: title+summary+content 한 번에 번역 */
    private TranslationResult translateSingle(String title, String protectedContent, String summary,
                                              String source, String target, Map<String, String> tagMap) {
        String systemPrompt = String.format(FULL_SYSTEM_PROMPT, source, target);
        String userMessage = String.format("""
                Title: %s

                Summary: %s

                Content:
                %s
                """, title, summary != null ? summary : "", protectedContent);

        JsonNode parsed = invokeAndParse(systemPrompt, userMessage);

        String translatedTitle = textOrThrow(parsed, "title");
        String translatedContent = parsed.hasNonNull("content") ? parsed.get("content").asText() : null;
        if (translatedContent == null) {
            throw new IllegalStateException("Model response missing 'content' field");
        }
        String translatedSummary = parsed.hasNonNull("summary") ? parsed.get("summary").asText() : null;

        return new TranslationResult(
                unescapeHtmlEntities(translatedTitle),
                restoreHtmlTags(translatedContent, tagMap),
                unescapeHtmlEntities(translatedSummary),
                modelId
        );
    }

    /** 긴 글: title/summary 1회 + content 청크별 번역 후 결합 */
    private TranslationResult translateChunked(String title, String protectedContent, String summary,
                                               String source, String target, Map<String, String> tagMap) {
        // 1) title + summary
        String titlePrompt = String.format(TITLE_SYSTEM_PROMPT, source, target);
        String titleMessage = String.format("Title: %s%n%nSummary: %s",
                title, summary != null ? summary : "");
        JsonNode titleParsed = invokeAndParse(titlePrompt, titleMessage);
        String translatedTitle = textOrThrow(titleParsed, "title");
        String translatedSummary = titleParsed.hasNonNull("summary") ? titleParsed.get("summary").asText() : null;

        // 2) content 청크 분할 (플레이스홀더는 절대 쪼개지 않음)
        List<String> chunks = splitByPlaceholders(protectedContent);
        log.info("청크 분할 번역: {} 개 청크 (청크당 최대 {}자)", chunks.size(), chunkSizeChars);

        String chunkPrompt = String.format(CHUNK_SYSTEM_PROMPT, source, target);
        StringBuilder translatedContent = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            JsonNode chunkParsed = invokeAndParse(chunkPrompt, chunks.get(i));
            String text = textOrThrow(chunkParsed, "text");
            translatedContent.append(text);
            log.info("청크 {}/{} 번역 완료 ({}자 → {}자)",
                    i + 1, chunks.size(), chunks.get(i).length(), text.length());
        }

        return new TranslationResult(
                unescapeHtmlEntities(translatedTitle),
                restoreHtmlTags(translatedContent.toString(), tagMap),
                unescapeHtmlEntities(translatedSummary),
                modelId
        );
    }

    /** Bedrock 호출 + 잘림 검사 + JSON 추출/파싱. 실패 시 예외. */
    private JsonNode invokeAndParse(String systemPrompt, String userMessage) {
        ConverseRequest converseRequest = ConverseRequest.builder()
                .modelId(modelId)
                .system(SystemContentBlock.builder().text(systemPrompt).build())
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText(userMessage))
                        .build())
                // temperature/top_p/top_k 는 Sonnet 5 이상에서 제거됨 (전송 시 ValidationException).
                // 출력 톤은 시스템 프롬프트로만 제어한다.
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(maxOutputTokens)
                        .build())
                .build();

        ConverseResponse response = bedrockRuntimeClient.converse(converseRequest);

        // 출력 토큰 한도로 잘린 응답은 불완전한 JSON → 즉시 실패 (재시도 대상)
        if (response.stopReason() == StopReason.MAX_TOKENS) {
            throw new IllegalStateException("Model response truncated (max_tokens reached)");
        }

        // Sonnet 5 이상은 adaptive thinking 이 기본 ON 이라 응답 첫 블록이 reasoningContent 일 수 있다.
        // content.get(0) 을 그대로 읽으면 번역문이 아닌 추론 블록을 집어 NPE 가 난다. 반드시 text 블록을 찾을 것.
        String responseText = response.output().message().content().stream()
                .map(ContentBlock::text)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Model response contained no text block"))
                .trim();
        String json = extractJson(responseText);
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Bedrock 응답 JSON 파싱 실패 (앞 200자): {}", json.substring(0, Math.min(200, json.length())));
            throw new IllegalStateException("Failed to parse model JSON response: " + e.getMessage(), e);
        }
    }

    /** 마크다운 코드펜스 제거 + 첫 '{'부터 마지막 '}'까지 추출 */
    private String extractJson(String text) {
        String cleaned = text
                .replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```\\s*$", "")
                .trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String textOrThrow(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            throw new IllegalStateException("Model response missing '" + field + "' field");
        }
        return node.get(field).asText();
    }

    /**
     * 플레이스홀더 경계 기준으로 청크 분할.
     * 플레이스홀더/텍스트 단위를 순서대로 누적하다가 chunkSizeChars 초과 시 새 청크 시작.
     */
    private List<String> splitByPlaceholders(String protectedContent) {
        List<String> units = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(protectedContent);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                units.add(protectedContent.substring(last, matcher.start()));
            }
            units.add(matcher.group());
            last = matcher.end();
        }
        if (last < protectedContent.length()) {
            units.add(protectedContent.substring(last));
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (current.length() > 0 && current.length() + unit.length() > chunkSizeChars) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(unit);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
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
        if (text == null || tagMap.isEmpty()) return unescapeHtmlEntities(text);
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
