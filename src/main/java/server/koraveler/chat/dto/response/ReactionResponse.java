// 7. ReactionResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReactionResponse {
    private String emoji;
    private Integer count;
    private List<UserSummary> users; // 반응한 사용자들
    private Boolean isMyReaction; // 현재 사용자가 반응했는지 여부
}