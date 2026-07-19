package server.nadeliv.travel.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 여행 프로젝트에서 다녀온 장소 (Korea Map 플러그인).
 * 장소 검색(카카오)으로 선택하며 시간·순서는 기록하지 않는다.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VisitedPlace {
    // 검색 제공자(카카오) 장소 ID — 중복 추가 방지 키
    private String id;
    private String name;
    private String nameEn;
    private String category;
    private String categoryEn;
    private String address;
    private Double lat;
    private Double lng;
    // 이 장소가 속한 시/군 행정코드 (프론트에서 좌표→지역 매칭 후 전달)
    private String regionCode;

    // ── 이전 장소 → 이 장소 이동 정보 (코스 타임라인, 사용자가 수동 입력) ──
    // CAR | TRANSIT | WALK
    private String transportMode;
    private Integer durationMinutes;
}
