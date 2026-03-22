package server.nadeliv.chat.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelEvent {
    private String eventType;
    private String channelId;
    private String userId;
    private Object data;
}
