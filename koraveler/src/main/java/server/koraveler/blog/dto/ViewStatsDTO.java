package server.koraveler.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ViewStatsDTO {
    private String documentId;
    private long totalViews;
    private long uniqueViews;           // IP 기준 유니크
    private long todayViews;
    private long weekViews;
    private List<DailyViewCount> dailyStats;  // 일별 통계

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyViewCount {
        private String date;
        private long count;
    }
}