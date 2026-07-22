package server.nadeliv.discovery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import server.nadeliv.discovery.model.entities.DiscoveredVideo;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Bedrock (Claude)로 수집된 유튜브 영상 메타데이터를 요약·태깅·랭킹.
 * BedrockPostTranslationClient 와 동일한 Converse API 패턴.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockVideoSummarizer {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cloud.aws.bedrock.model-id}")
    private String modelId;

    private static final String SYSTEM_PROMPT = """
            You are a travel content curator for a travel blog platform.
            You are given YouTube video metadata (title, description, channel, view count) about a travel destination.

            TASKS:
            1. Write an overall destination digest (3-5 sentences): what travelers focus on for this destination based on these videos (popular spots, food, itinerary patterns, practical tips).
            2. For EACH video: a one-line summary of what it covers, 1-3 tags, and a recommendation score 0-100 (consider relevance to travel planning, information density implied by title/description, and view count).

            RULES:
            - Write ALL text (digest and one-liners) in %s.
            - Tags must be chosen ONLY from: itinerary, food, spots, transport, budget, accommodation, tips, vlog.
            - If a video is clearly unrelated to travel in "%s", give it score 0.
            - Respond ONLY in this JSON format without any markdown or code blocks:
            {"summary": "...", "videos": [{"videoId": "...", "oneLiner": "...", "tags": ["..."], "score": 85}]}
            """;

    public record CurationResult(String summary, List<DiscoveredVideo> videos) {}

    /**
     * 영상 메타데이터 목록을 큐레이션. score 내림차순으로 정렬해 반환.
     */
    public CurationResult curate(String destination, String locale,
                                 List<YouTubeSearchClient.VideoMeta> videoMetas) {
        try {
            String systemPrompt = String.format(SYSTEM_PROMPT, languageName(locale), destination);

            StringBuilder sb = new StringBuilder();
            sb.append("Destination: ").append(destination).append("\n\nVideos:\n");
            for (YouTubeSearchClient.VideoMeta v : videoMetas) {
                sb.append(String.format("- videoId: %s | title: %s | channel: %s | views: %s%n  description: %s%n",
                        v.videoId(), v.title(), v.channelTitle(),
                        v.viewCount() != null ? v.viewCount() : "?",
                        truncate(v.description(), 300)));
            }

            ConverseRequest request = ConverseRequest.builder()
                    .modelId(modelId)
                    .system(SystemContentBlock.builder().text(systemPrompt).build())
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(sb.toString()))
                            .build())
                    .build();

            ConverseResponse response = bedrockRuntimeClient.converse(request);
            String responseText = response.output().message().content().get(0).text().trim();
            return parse(responseText, videoMetas);

        } catch (Exception e) {
            log.error("Bedrock 영상 큐레이션 실패: destination={}, error={}", destination, e.getMessage());
            throw new RuntimeException("Video curation failed: " + e.getMessage(), e);
        }
    }

    private CurationResult parse(String responseText, List<YouTubeSearchClient.VideoMeta> videoMetas) throws Exception {
        // 모델이 코드블록으로 감싸는 경우 방어
        String cleaned = responseText
                .replaceAll("(?s)^```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "");
        JsonNode root = objectMapper.readTree(cleaned);

        Map<String, JsonNode> curatedById = new HashMap<>();
        for (JsonNode v : root.path("videos")) {
            curatedById.put(v.path("videoId").asText(), v);
        }

        List<DiscoveredVideo> videos = new ArrayList<>();
        for (YouTubeSearchClient.VideoMeta meta : videoMetas) {
            JsonNode curated = curatedById.get(meta.videoId());
            List<String> tags = new ArrayList<>();
            if (curated != null) {
                curated.path("tags").forEach(t -> tags.add(t.asText()));
            }
            videos.add(DiscoveredVideo.builder()
                    .videoId(meta.videoId())
                    .title(meta.title())
                    .channelTitle(meta.channelTitle())
                    .publishedAt(meta.publishedAt())
                    .thumbnailUrl(meta.thumbnailUrl())
                    .viewCount(meta.viewCount())
                    .duration(meta.duration())
                    .aiSummary(curated != null ? curated.path("oneLiner").asText(null) : null)
                    .tags(tags)
                    .score(curated != null ? curated.path("score").asInt(0) : 0)
                    .build());
        }
        videos.sort((a, b) -> Integer.compare(
                b.getScore() != null ? b.getScore() : 0,
                a.getScore() != null ? a.getScore() : 0));

        return new CurationResult(root.path("summary").asText(null), videos);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private String languageName(String code) {
        return switch (code) {
            case "ko" -> "Korean";
            case "zh" -> "Chinese (Simplified)";
            case "ja" -> "Japanese";
            default -> "English";
        };
    }
}
