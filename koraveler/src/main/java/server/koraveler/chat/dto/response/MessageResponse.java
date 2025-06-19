// 1. MessageResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.enums.MessageStatus;
import server.koraveler.chat.model.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageResponse {
    private String id;
    private String channelId;
    private String message;
    private String userId;
    private String userNickname; // 조인해서 가져올 데이터
    private String userAvatarUrl; // 조인해서 가져올 데이터
    private MessageType messageType;
    private MessageStatus status;
    private String parentMessageId;
    private MessageResponse parentMessage; // 답글인 경우 원본 메시지
    private List<AttachmentResponse> attachments;
    private String thumbnailUrl;
    private Boolean isEdited;
    private Boolean isPinned;
    private List<String> mentionedUserIds;
    private List<UserSummary> mentionedUsers; // 멘션된 사용자 정보
    private Boolean isSystemMessage;
    private List<ReactionResponse> reactions;
    private Integer unreadCount; // 이 메시지를 안 읽은 사람 수
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}