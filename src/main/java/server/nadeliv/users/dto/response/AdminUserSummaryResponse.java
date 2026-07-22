// AdminUserSummaryResponse.java
package server.nadeliv.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {
    private String id;
    private String userId;
    private String name;
    private String email;
    private String src;              // 프로필 이미지
    private List<String> roles;
    private boolean enabled;         // false = 탈퇴(비활성)
    private LocalDateTime created;   // 가입일
    private LocalDateTime updated;
}
