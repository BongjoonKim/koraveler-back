package server.nadeliv.travel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.nadeliv.travel.model.enums.ChannelContextType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelChannelResponse {
    // TravelChannel 필드
    private String id;
    private String travelId;
    private String channelId;
    private ChannelContextType contextType;
    private String contextId;
    private String channelPurpose;
    private Boolean isPinned;
    private Integer displayOrder;

    // Channel 필드 (기존 Chat 모듈에서 조회)
    private String channelName;
    private String channelDescription;
    private Integer memberCount;
    private LocalDateTime lastMessageAt;

    // 감사 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
