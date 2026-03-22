package server.nadeliv.travel.model.mapper;

import org.springframework.stereotype.Component;
import server.nadeliv.chat.dto.request.ChannelCreateRequest;
import server.nadeliv.chat.dto.response.ChannelResponse;
import server.nadeliv.chat.model.enums.ChannelType;
import server.nadeliv.travel.model.dto.TravelChannelCreateRequest;
import server.nadeliv.travel.model.dto.TravelChannelResponse;
import server.nadeliv.travel.model.entities.TravelChannel;
import server.nadeliv.travel.model.enums.ChannelContextType;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TravelChannelMapper {

    // TravelChannelCreateRequest → TravelChannel 엔티티
    public TravelChannel toEntity(TravelChannelCreateRequest request, String travelId, String channelId, String userId) {
        TravelChannel entity = TravelChannel.builder()
                .travelId(travelId)
                .channelId(channelId)
                .contextType(request.getContextType() != null ? request.getContextType() : ChannelContextType.GENERAL)
                .contextId(request.getContextId())
                .channelPurpose(request.getChannelPurpose())
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isDeleted(false)
                .build();
        entity.setCreated(LocalDateTime.now());
        entity.setUpdated(LocalDateTime.now());
        entity.setCreatedUser(userId);
        entity.setUpdatedUser(userId);
        return entity;
    }

    // TravelChannel + ChannelResponse → TravelChannelResponse
    public TravelChannelResponse toResponse(TravelChannel entity, ChannelResponse channelResponse) {
        TravelChannelResponse.TravelChannelResponseBuilder builder = TravelChannelResponse.builder()
                .id(entity.getId())
                .travelId(entity.getTravelId())
                .channelId(entity.getChannelId())
                .contextType(entity.getContextType())
                .contextId(entity.getContextId())
                .channelPurpose(entity.getChannelPurpose())
                .isPinned(entity.getIsPinned())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreated())
                .updatedAt(entity.getUpdated());

        if (channelResponse != null) {
            builder.channelName(channelResponse.getName())
                    .channelDescription(channelResponse.getDescription())
                    .memberCount(channelResponse.getMemberCount())
                    .lastMessageAt(channelResponse.getLastMessageAt());
        }

        return builder.build();
    }

    // TravelChannelCreateRequest → ChannelCreateRequest (Chat 모듈용)
    public ChannelCreateRequest toChannelCreateRequest(TravelChannelCreateRequest request, List<String> memberUserIds) {
        return ChannelCreateRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .channelType(ChannelType.GROUP)
                .initialMemberIds(memberUserIds)
                .build();
    }
}
