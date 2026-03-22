package server.nadeliv.travel.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.common.dto.CommonDTO;
import server.nadeliv.travel.model.enums.ChannelContextType;

@Document(collection = "travel_channels")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelChannel extends CommonDTO {
    @Id
    private String id;

    @Indexed
    private String travelId;

    @Indexed
    private String channelId;

    // 채널 컨텍스트
    private ChannelContextType contextType;
    private String contextId;
    private String channelPurpose;

    // 표시 설정
    @Builder.Default
    private Boolean isPinned = false;
    @Builder.Default
    private Integer displayOrder = 0;

    // Soft Delete
    @Builder.Default
    private Boolean isDeleted = false;
}
