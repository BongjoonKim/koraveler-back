// 6. ChannelJoinRequest.java
package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelJoinRequest {
    private String password; // 비공개 채널용
    private String inviteCode; // 초대 링크용
    private String nickname; // 채널 내 닉네임
}