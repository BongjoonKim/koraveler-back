// AdminRoleUpdateRequest.java
package server.nadeliv.users.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleUpdateRequest {
    // 부여할 권한 전체 목록 (예: ["user"] 또는 ["user", "admin"])
    private List<String> roles;
}
