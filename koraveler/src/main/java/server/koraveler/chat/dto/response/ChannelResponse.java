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
    private Integer unreadMessageCount; // 사용자별 안 읽은 메시지 수
    private MessageResponse lastMessage; // 마지막 메시지
    private LocalDateTime lastMessageAt;
    private Boolean isArchived;
    private Boolean isReadOnly;
    private Boolean isMember; // 현재 사용자의 멤버 여부
    private Boolean isMuted; // 현재 사용자의 음소거 여부
    private String myRole; // 현재 사용자의 역할
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}