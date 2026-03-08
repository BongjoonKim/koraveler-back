package server.koraveler.i18n.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 재번역 요청 (수동 편집된 번역 덮어쓰기 확인용)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetranslateRequestDTO {
    private boolean confirmed;
}
