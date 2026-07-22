// AdminStatusUpdateRequest.java
package server.nadeliv.users.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatusUpdateRequest {
    // false = 탈퇴 처리(soft), true = 계정 복구
    private Boolean enabled;
}
