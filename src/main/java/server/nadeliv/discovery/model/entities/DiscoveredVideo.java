package server.nadeliv.discovery.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 목적지 다이제스트에 임베드되는 수집 영상 1건.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredVideo {
    private String videoId;
    private String title;
    private String channelTitle;
    private String publishedAt;
    private String thumbnailUrl;
    private Long viewCount;
    private String duration;      // ISO8601 (예: PT12M34S)

    // AI 큐레이션 결과
    private String aiSummary;     // 한 줄 요약
    private List<String> tags;    // 예: food, itinerary, transport
    private Integer score;        // 추천도 0~100 (정렬 기준)
}
