package server.nadeliv.discovery.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.common.dto.CommonDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 목적지 단위 여행 정보 큐레이션 결과.
 * (destinationKey, locale) 별로 1건씩 캐싱되어 모든 사용자가 공유.
 */
@Document(collection = "destination_digests")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CompoundIndex(name = "idx_destination_locale", def = "{'destinationKey': 1, 'locale': 1}", unique = true)
public class DestinationDigest extends CommonDTO {
    @Id
    private String id;

    private String destinationKey;   // 정규화된 검색어 (소문자, 공백 정리)
    private String displayName;      // 사용자가 입력한 원래 검색어
    private String locale;           // 요약 언어 ("ko" | "en" | "zh" | "ja")

    private String summary;          // 목적지 전체 AI 요약
    private List<DiscoveredVideo> videos;

    private LocalDateTime collectedAt;
}
