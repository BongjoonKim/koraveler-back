// 8. UserSummary.java (공통으로 사용할 사용자 요약 정보)
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSummary {
    private String id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Boolean isOnline;
}