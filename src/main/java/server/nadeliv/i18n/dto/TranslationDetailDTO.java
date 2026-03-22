package server.nadeliv.i18n.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 작성자용: 특정 번역본 상세 (편집용)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationDetailDTO {
    private String postId;
    private String locale;
    private String title;
    private String content;
    private String summary;
    private String status;       // TranslationStatus
    private String translatedBy; // TranslatedBy
    private String aiModel;
    private String updatedAt;
}
