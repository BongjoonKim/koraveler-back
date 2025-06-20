// 3. ChannelResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.enums.ChannelType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelResponse {
    private String id;
    private String name;
    private String description;
    private ChannelType channelType;
    private String avatarUrl;
    private String topic;
    private List<String> tags;
    private Integer memberCount;
    private LocalDateTime lastMessageAt;
    private Boolean isArchived;
    private Boolean isReadOnly;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isMember;
    private Boolean isMuted;
    private Integer unreadMessageCount;
    private String myRole;
    private MessageResponse lastMessage;
}