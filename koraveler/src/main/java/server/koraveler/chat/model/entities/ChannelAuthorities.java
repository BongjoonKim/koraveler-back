// 1. ChannelAuthorities.java (Entity)
package server.koraveler.chat.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "channel_authorities")
public class ChannelAuthorities {
    @Id
    private String id;
    private String channelId;
    private String userId;
    private String roleId;
    private List<String> permissions;
    private String grantedByUserId;
    private LocalDateTime grantedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}