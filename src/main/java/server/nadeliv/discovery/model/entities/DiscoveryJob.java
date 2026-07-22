package server.nadeliv.discovery.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.discovery.model.enums.DiscoveryJobStatus;

import java.time.LocalDateTime;

/**
 * 목적지 정보 수집 비동기 작업. TranslationJob 과 동일한 폴링 워커 패턴.
 */
@Document(collection = "discovery_jobs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': 1}")
public class DiscoveryJob {
    @Id
    private String id;

    private String destinationKey;
    private String query;            // 사용자가 입력한 원래 검색어
    private String locale;

    private String requestedUser;    // 수집을 트리거한 사용자 id

    @Builder.Default
    private DiscoveryJobStatus status = DiscoveryJobStatus.QUEUED;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 2;

    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
