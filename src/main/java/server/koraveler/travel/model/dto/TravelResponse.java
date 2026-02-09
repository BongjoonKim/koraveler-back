package server.koraveler.travel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.travel.model.embedded.TravelSchedule;
import server.koraveler.travel.model.enums.TravelStatus;
import server.koraveler.travel.model.enums.TravelVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelResponse {
    private String id;
    private String title;
    private String description;
    private String coverImageUrl;
    private TravelVisibility visibility;
    private TravelStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String destination;
    private List<String> tags;
    private List<TravelSchedule> schedules;
    private List<String> channelIds;
    private List<TravelMemberResponse> members;
    private Integer memberCount;
    private String createdUser;
    private LocalDateTime created;
    private LocalDateTime updated;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TravelMemberResponse {
        private String userId;
        private String role;
        private String nickname;
        private LocalDateTime joinedAt;
    }
}
