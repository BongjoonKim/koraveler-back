package server.nadeliv.discovery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.nadeliv.discovery.model.entities.DestinationDigest;
import server.nadeliv.discovery.model.enums.DiscoveryJobStatus;

/**
 * 다이제스트 조회/수집 요청 공통 응답.
 * digest 가 null 이면 아직 수집된 데이터 없음. jobStatus 로 수집 진행 여부 판단.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryDigestResponse {
    private DestinationDigest digest;
    private DiscoveryJobStatus jobStatus;   // 진행 중/최근 작업 상태 (없으면 null)
    private String errorMessage;            // jobStatus == FAILED 일 때 원인
}
