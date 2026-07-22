package server.nadeliv.discovery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YouTube Data API v3 클라이언트.
 * search.list (100 units) + videos.list (1 unit) 로 목적지 관련 여행 영상 수집.
 * 쿼터가 크므로 결과는 반드시 목적지 단위로 캐싱해서 사용할 것 (DestinationDigest).
 */
@Slf4j
@Component
public class YouTubeSearchClient {

    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final int MAX_RESULTS = 12;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.youtube.api-key:}")
    private String apiKey;

    public record VideoMeta(String videoId, String title, String description, String channelTitle,
                            String publishedAt, String thumbnailUrl, Long viewCount, String duration) {}

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 목적지 키워드로 여행 영상 검색 후 조회수/재생시간까지 채워서 반환.
     */
    public List<VideoMeta> searchTravelVideos(String query, String locale) {
        if (!isConfigured()) {
            throw new IllegalStateException("YouTube API key is not configured (spring.youtube.api-key)");
        }

        List<VideoMeta> videos = search(query + travelSuffix(locale), locale);
        if (videos.isEmpty()) return videos;

        Map<String, JsonNode> details = fetchVideoDetails(
                videos.stream().map(VideoMeta::videoId).toList());

        List<VideoMeta> enriched = new ArrayList<>();
        for (VideoMeta v : videos) {
            JsonNode detail = details.get(v.videoId());
            Long viewCount = null;
            String duration = null;
            if (detail != null) {
                viewCount = detail.path("statistics").path("viewCount").asLong(0);
                duration = detail.path("contentDetails").path("duration").asText(null);
            }
            enriched.add(new VideoMeta(v.videoId(), v.title(), v.description(), v.channelTitle(),
                    v.publishedAt(), v.thumbnailUrl(), viewCount, duration));
        }
        return enriched;
    }

    private List<VideoMeta> search(String q, String locale) {
        URI uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", q)
                .queryParam("type", "video")
                .queryParam("videoEmbeddable", "true")
                .queryParam("maxResults", MAX_RESULTS)
                .queryParam("relevanceLanguage", locale)
                .queryParam("key", apiKey)
                .encode()
                .build()
                .toUri();

        try {
            String body = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(body);

            List<VideoMeta> results = new ArrayList<>();
            for (JsonNode item : root.path("items")) {
                String videoId = item.path("id").path("videoId").asText(null);
                if (videoId == null) continue;
                JsonNode snippet = item.path("snippet");
                results.add(new VideoMeta(
                        videoId,
                        snippet.path("title").asText(""),
                        snippet.path("description").asText(""),
                        snippet.path("channelTitle").asText(""),
                        snippet.path("publishedAt").asText(""),
                        snippet.path("thumbnails").path("medium").path("url").asText(null),
                        null,
                        null
                ));
            }
            log.info("YouTube 검색 완료: q='{}', {} 건", q, results.size());
            return results;
        } catch (Exception e) {
            log.error("YouTube 검색 실패: q='{}', error={}", q, e.getMessage());
            throw new RuntimeException("YouTube search failed: " + e.getMessage(), e);
        }
    }

    private Map<String, JsonNode> fetchVideoDetails(List<String> videoIds) {
        URI uri = UriComponentsBuilder.fromHttpUrl(VIDEOS_URL)
                .queryParam("part", "statistics,contentDetails")
                .queryParam("id", String.join(",", videoIds))
                .queryParam("key", apiKey)
                .encode()
                .build()
                .toUri();

        Map<String, JsonNode> byId = new HashMap<>();
        try {
            String body = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(body);
            for (JsonNode item : root.path("items")) {
                byId.put(item.path("id").asText(), item);
            }
        } catch (Exception e) {
            // 상세 조회 실패는 치명적이지 않음 — 조회수/재생시간 없이 진행
            log.warn("YouTube 영상 상세 조회 실패 (무시하고 진행): {}", e.getMessage());
        }
        return byId;
    }

    private String travelSuffix(String locale) {
        return switch (locale) {
            case "ko" -> " 여행";
            case "ja" -> " 旅行";
            case "zh" -> " 旅游";
            default -> " travel";
        };
    }
}
