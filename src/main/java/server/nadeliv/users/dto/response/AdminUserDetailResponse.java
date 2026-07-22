// AdminUserDetailResponse.java
package server.nadeliv.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
    private AdminUserSummaryResponse user;

    // 작성한 글 (페이징)
    private Long documentCount;
    private List<DocumentSummary> documents;
    private Integer documentPage;
    private Integer documentTotalPages;

    // 참여 중인 여행 프로젝트
    private Long travelCount;
    private List<TravelSummary> travels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSummary {
        private String id;
        private String title;
        private boolean draft;
        private boolean disclose;
        private boolean deleted;        // 휴지통 여부
        private String thumbnailImgUrl;
        private List<String> tags;
        private LocalDateTime created;
        private LocalDateTime updated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TravelSummary {
        private String id;
        private String title;
        private String destination;
        private String status;          // PLANNING / IN_PROGRESS / COMPLETED
        private String visibility;
        private LocalDate startDate;
        private LocalDate endDate;
        private String role;            // 해당 여행에서의 역할 (ADMIN / USER)
        private Long memberCount;
        private LocalDateTime created;
    }
}
