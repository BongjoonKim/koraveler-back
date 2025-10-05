package server.koraveler.translate.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.CommonDTO;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "translation_history")
public class TranslationHistory extends CommonDTO {
    @Id
    private String id;

    @Indexed
    private String userId;  // 소유자
    private String sourceText;
    private String targetText;
    private String sourceLanguage;
    private String targetLanguage;

    @Builder.Default
    private boolean isLiked = false;

    @Indexed(expireAfterSeconds = 2592000)
    private LocalDateTime expireAt;

    // 좋아요를 누른 항목은 expireAt을 null로 설정
    public void markAsLiked() {
        this.isLiked = true;
        this.expireAt = null;
    }
}
