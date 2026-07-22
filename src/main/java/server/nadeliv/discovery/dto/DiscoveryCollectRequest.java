package server.nadeliv.discovery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryCollectRequest {
    private String query;    // 목적지 검색어 (예: "sydney", "시드니")
    private String locale;   // 요약 언어 (기본 "en")
}
