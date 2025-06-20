// 2. ChannelInvitations.java (Entity)
package server.koraveler.chat.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.chat.model.enums.InvitationStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "channel_invitations")
public class ChannelInvitations {
    @Id
    private String id;
    private String channelId;
    private String inviteCode;
    private String createdByUserId;
    private InvitationStatus status;
    private Integer maxUses;
    private Integer currentUses;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}