// 5. ChannelMemberResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.model.enums.NotificationLevel;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelMemberResponse {
    private String id;
    private String userId;
    private String nickname;
    private String userRealName; // 조인해서 가져올 데이터
    private String userAvatarUrl; // 조인해서 가져올 데이터
    private MemberStatus status;
    private String role; // 권한 정보
    private Boolean isOnline; // 온라인 상태
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeenAt;
    private String lastReadMessageId;
    private NotificationLevel notificationLevel;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
}