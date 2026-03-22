package server.nadeliv.i18n.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import server.nadeliv.i18n.model.enums.TranslatedBy;
import server.nadeliv.i18n.model.enums.TranslationStatus;

import java.time.LocalDateTime;

@Document(collection = "post_translations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "idx_post_locale", def = "{'postId': 1, 'locale': 1}", unique = true),
        @CompoundIndex(name = "idx_locale_status", def = "{'locale': 1, 'status': 1}")
})
public class PostTranslation {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String postId;

    private String locale;  // "en" | "zh" | "ja"

    private String title;
    private String content;  // HTML
    private String summary;

    @Builder.Default
    private TranslationStatus status = TranslationStatus.PENDING;

    @Builder.Default
    private TranslatedBy translatedBy = TranslatedBy.AI;

    private String aiModel;  // "anthropic.claude-3-sonnet"

    private LocalDateTime manuallyEditedAt;
    private String manuallyEditedBy;  // userId

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
