package server.koraveler.chat.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageRequest {

    // ✅ 페이지 번호 필드 추가 (0부터 시작)
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private Integer size = 20;

    private String cursor; // 커서 기반 페이징용 (선택적)

    private String sortBy = "createdAt";

    private String sortDirection = "desc";

    // 편의 메서드들
    public Integer getPage() {
        return page != null ? page : 0;
    }

    public Integer getSize() {
        return size != null ? size : 20;
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public String getSortDirection() {
        return sortDirection != null ? sortDirection : "desc";
    }
}