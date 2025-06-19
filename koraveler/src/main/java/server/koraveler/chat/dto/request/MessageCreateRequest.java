package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.enums.MessageType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageCreateRequest {
    @NotBlank(message = "채널 ID는 필수입니다")
    private String channelId;

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 2000, message = "메시지는 2000자를 초과할 수 없습니다")
    private String message;

    @NotNull(message = "메시지 타입은 필수입니다")
    private MessageType messageType;

    private String parentMessageId; // 답글용
    private List<AttachmentRequest> attachments;
    private List<String> mentionedUserIds;
    private String clientId; // 중복 방지용
}