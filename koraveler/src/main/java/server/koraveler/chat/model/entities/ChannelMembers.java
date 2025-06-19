package server.koraveler.chat.model.entities;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.model.enums.NotificationLevel;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "channel_members")
public class ChannelMembers {
    @Id
    private String id;
    private String userId;
    private String channelId;
    private MemberStatus status;
    private String nickname;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeenAt;
    private String lastReadMessageId;
    private Boolean notificationsEnabled;
    private NotificationLevel notificationLevel;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime leftAt;
}
