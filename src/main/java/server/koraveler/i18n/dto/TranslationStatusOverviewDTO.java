package server.koraveler.i18n.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 작성자용: 전체 번역 상태 조회 응답
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationStatusOverviewDTO {
    private String postId;
    private String originalLocale;
    private List<TranslationStatusItemDTO> translations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranslationStatusItemDTO {
        private String locale;
        private String status;
        private String translatedBy;
        private String aiModel;
        private String manuallyEditedAt;
        private String updatedAt;
    }
}
