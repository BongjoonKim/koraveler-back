// 2. ChannelMapper.java
package server.koraveler.chat.dto.mapper;

import org.springframework.stereotype.Component;
import server.koraveler.chat.dto.request.ChannelCreateRequest;
import server.koraveler.chat.dto.response.ChannelResponse;
import server.koraveler.chat.model.entity.Channels;

import java.time.LocalDateTime;

@Component
public class ChannelMapper {

    public Channels toEntity(ChannelCreateRequest request, String userId) {
        return Channels.builder()
                .name(request.getName())
                .description(request.getDescription())
                .channelType(request.getChannelType())
                .createdUserId(userId)
                .updatedUserId(userId)
                .password(request.getPassword()) // 실제로는 암호화해서 저장
                .requiresApproval(request.getRequiresApproval() != null ?
                        request.getRequiresApproval() : false)
                .maxMembers(request.getMaxMembers())
                .topic(request.getTopic())
                .tags(request.getTags())
                .memberCount(1) // 생성자 포함
                .isArchived(false)
                .isReadOnly(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public ChannelResponse toResponse(Channels entity) {
        return ChannelResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .channelType(entity.getChannelType())
                .avatarUrl(entity.getAvatarUrl())
                .topic(entity.getTopic())
                .tags(entity.getTags())
                .memberCount(entity.getMemberCount())
                .lastMessageAt(entity.getLastMessageAt())
                .isArchived(entity.getIsArchived())
                .isReadOnly(entity.getIsReadOnly())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}