package server.nadeliv.travel.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대시보드에 표시할 항목 1건 (Travels 에 임베드).
 * key 는 프론트 위젯 레지스트리(travelDashboardItems.ts)의 키.
 * 배열 순서가 대시보드 표시 순서.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardItem {
    private String key;
    private String display;   // "stat"(작은 박스) | "row"(전체 행 섹션)
    private boolean visible;
}
