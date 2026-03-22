package server.nadeliv.i18n.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 작성자용: 번역본 수동 수정 요청
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationEditRequestDTO {
    private String title;
    private String content;
    private String summary;
}
