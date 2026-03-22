package server.nadeliv.travel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelListResponse {
    private List<TravelResponse> travels;
    private PaginationInfo pagination;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PaginationInfo {
        private Long totalCount;
        private Integer pageSize;
        private Integer currentPage;
        private Boolean hasMore;
    }
}
