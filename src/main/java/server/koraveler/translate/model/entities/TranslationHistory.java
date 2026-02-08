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
    private String userId;

    private String sourceText;
    private String targetText;
    private String sourceLanguage;
    private String targetLanguage;

    // 발음 정보 (라틴 문자로 표기)
    private String pronunciation;

    @Builder.Default
    private boolean isLiked = false;

    @Indexed(expireAfterSeconds = 2592000)
    private LocalDateTime expireAt;

    public void markAsLiked() {
        this.isLiked = true;
        this.expireAt = null;
    }
}