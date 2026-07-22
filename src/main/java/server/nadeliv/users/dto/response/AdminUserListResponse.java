// AdminUserListResponse.java
package server.nadeliv.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {
    private List<AdminUserSummaryResponse> users;
    private Long totalCount;      // 현재 필터(검색어·상태) 기준 총 수
    private Integer totalPages;
    private Integer currentPage;
    private Boolean hasNext;

    // 필터와 무관한 전체 통계
    private Long totalAll;
    private Long activeCount;
    private Long disabledCount;
}
