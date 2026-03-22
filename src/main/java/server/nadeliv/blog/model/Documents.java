package server.nadeliv.blog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.common.dto.CommonDTO;
import server.nadeliv.common.dto.UserCommon;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "documents")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Documents extends CommonDTO {
    @Id
    private String id;
    private String title;
    private String contents;
    private String contentsType;
    @Builder.Default
    private boolean disclose = true;
    private List<String> tags;
    private String folderId;
    private String color;
    private String thumbnailImgUrl;
    private boolean draft;

    // i18n: 원본 언어 (기본값 "ko")
    @Builder.Default
    private String originalLocale = "ko";

    // Featured 관련 필드 추가
    private boolean featuredReady;  // Featured 가능 여부
    private FeaturedInfo featuredInfo;  // Featured 전용 정보
    private FeaturedSchedule featuredSchedule;  // Featured 스케줄 정보


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeaturedInfo {
        private String featuredTitle;  // Featured용 제목
        private String featuredSubtitle;  // 부제목
        private String featuredImageUrl;  // 히어로 이미지
        private String featuredGradientFrom;  // 그라디언트 시작 색상
        private String featuredGradientTo;  // 그라디언트 끝 색상
        private String location;  // 위치 정보
        private List<String> highlights;  // 하이라이트 태그
        private String ctaButtonText;  // CTA 버튼 텍스트
        private Integer displayPriority;  // 표시 우선순위
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeaturedSchedule {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private boolean isActive;
        private String approvedBy;  // 승인한 관리자
        private LocalDateTime approvedAt;
    }
}
