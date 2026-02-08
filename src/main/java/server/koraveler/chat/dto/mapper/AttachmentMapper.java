// 10. AttachmentMapper.java (Helper for MessageMapper)
package server.koraveler.chat.dto.mapper;

import server.koraveler.chat.dto.request.AttachmentRequest;
import server.koraveler.chat.dto.response.AttachmentResponse;
import server.koraveler.chat.model.embedded.MessageAttachment;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AttachmentMapper {

    public static List<MessageAttachment> toEmbeddedList(List<AttachmentRequest> requests) {
        if (requests == null) return List.of();

        return requests.stream()
                .map(AttachmentMapper::toEmbedded)
                .collect(Collectors.toList());
    }

    public static MessageAttachment toEmbedded(AttachmentRequest request) {
        return MessageAttachment.builder()
                .id(UUID.randomUUID().toString())
                .fileName(request.getFileName())
                .originalFileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .mimeType(request.getMimeType())
                .fileSize(request.getFileSize())
                .width(request.getWidth())
                .height(request.getHeight())
                .duration(request.getDuration())
                .build();
    }

    public static List<AttachmentResponse> toResponseList(List<MessageAttachment> attachments) {
        if (attachments == null) return List.of();

        return attachments.stream()
                .map(AttachmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    public static AttachmentResponse toResponse(MessageAttachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .fileUrl(attachment.getFileUrl())
                .mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize())
                .width(attachment.getWidth())
                .height(attachment.getHeight())
                .duration(attachment.getDuration())
                .build();
    }
}