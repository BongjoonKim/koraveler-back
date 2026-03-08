package server.koraveler.i18n.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import server.koraveler.i18n.model.enums.JobStatus;

import java.time.LocalDateTime;

@Document(collection = "translation_jobs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': 1}")
public class TranslationJob {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String postId;

    private String targetLocale;  // "en" | "zh" | "ja"

    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 3;

    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
