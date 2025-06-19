// 1. MessageMapper.java
package server.koraveler.chat.dto.mapper;

import org.springframework.stereotype.Component;
import server.koraveler.chat.dto.request.MessageCreateRequest;
import server.koraveler.chat.dto.response.MessageResponse;
import server.koraveler.chat.model.entity.Messages;
import server.koraveler.chat.model.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class MessageMapper {

    public Messages toEntity(MessageCreateRequest request, String userId) {
        return Messages.builder()
                .channelId(request.getChannelId())
                .message(request.getMessage())
                .userId(userId)
                .messageType(request.getMessageType())
                .parentMessageId(request.getParentMessageId())
                .mentionedUserIds(request.getMentionedUserIds())
                .attachments(request.getAttachments() != null ?
                        AttachmentMapper.toEmbeddedList(request.getAttachments()) : new ArrayList<>())
                .status(MessageStatus.SENT)
                .isEdited(false)
                .isDeleted(false)
                .isPinned(false)
                .isSystemMessage(false)
                .reactions(new ArrayList<>())
                .clientId(request.getClientId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public MessageResponse toResponse(Messages entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .channelId(entity.getChannelId())
                .message(entity.getMessage())
                .userId(entity.getUserId())
                .messageType(entity.getMessageType())
                .status(entity.getStatus())
                .parentMessageId(entity.getParentMessageId())
                .attachments(AttachmentMapper.toResponseList(entity.getAttachments()))
                .thumbnailUrl(entity.getThumbnailUrl())
                .isEdited(entity.getIsEdited())
                .isPinned(entity.getIsPinned())
                .mentionedUserIds(entity.getMentionedUserIds())
                .isSystemMessage(entity.getIsSystemMessage())
                .reactions(ReactionMapper.toResponseList(entity.getReactions()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}