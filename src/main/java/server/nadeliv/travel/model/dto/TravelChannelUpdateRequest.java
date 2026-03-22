package server.nadeliv.travel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.nadeliv.travel.model.enums.ChannelContextType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelChannelUpdateRequest {
    private ChannelContextType contextType;
    private String contextId;
    private String channelPurpose;
    private Boolean isPinned;
    private Integer displayOrder;
}
