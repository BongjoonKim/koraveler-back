package server.koraveler.chat.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.chat.model.embedded.MessageAttachment;
import server.koraveler.chat.model.embedded.MessageReaction;
import server.koraveler.chat.model.enums.MessageStatus;
import server.koraveler.chat.model.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "messages")
public class Messages {
    @Id
    private String id;
    private String channelId;
    private String message;
    private String userId;
    private MessageType messageType;
    private String parentMessageId;
    private List<MessageAttachment> attachments;
    private String thumbnailUrl;
    private MessageStatus status;
    private Boolean isEdited;
    private Boolean isDeleted;
    private Boolean isPinned;
    private List<String> mentionedUserIds;
    private Boolean isSystemMessage;
    private List<MessageReaction> reactions;
    private String clientId;
    private String userAgent;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}