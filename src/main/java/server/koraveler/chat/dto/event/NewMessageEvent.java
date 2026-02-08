package server.koraveler.chat.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageEvent {
    private String channelId;
    private String messageId;
    private String senderId;
}
