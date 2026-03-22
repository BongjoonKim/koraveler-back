// 5. ChannelMemberResponse.java
package server.nadeliv.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.nadeliv.chat.model.enums.MemberStatus;
import server.nadeliv.chat.model.enums.NotificationLevel;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelMemberResponse {
    private String id;
    private String userId;
    private String nickname;
    private MemberStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeenAt;
    private String lastReadMessageId;
    private NotificationLevel notificationLevel;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
    private UserSummary user;
    private String roleId;
    private String roleName;
    private Boolean isOnline;
}