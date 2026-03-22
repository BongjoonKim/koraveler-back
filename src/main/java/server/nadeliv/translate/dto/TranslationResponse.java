// TranslationResponse.java
package server.nadeliv.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {
    private String id;
    private String sourceText;
    private String targetText;
    private String sourceLanguage;
    private String targetLanguage;
    private LocalDateTime translatedAt;
    private boolean fromCache;
    private boolean isLiked;

    // 추가 정보
    private String pronunciation;  // 발음 (한국어/중국어/일본어 등)
    private Double confidence;     // 번역 신뢰도 점수
    private String[] alternatives; // 대체 번역들
}