// UserSearchResponse.java
package server.koraveler.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSearchResponse {
    private List<UserResponse> users;
    private Integer totalCount;
    private Boolean hasNext;
    private Integer currentPage;  // 옵션
    private Integer totalPages;   // 옵션
}