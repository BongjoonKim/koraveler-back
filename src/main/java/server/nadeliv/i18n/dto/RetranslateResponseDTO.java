package server.nadeliv.i18n.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 재번역 응답 (수동 편집된 번역일 경우 경고 포함)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetranslateResponseDTO {
    private String warning;
    private boolean requiresConfirmation;
}
