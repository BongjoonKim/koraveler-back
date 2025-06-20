package server.koraveler.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.enums.ChannelType;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelCreateRequest {
    @NotBlank(message = "채널명은 필수입니다")
    @Size(min = 2, max = 50, message = "채널명은 2-50자 사이여야 합니다")
    private String name;

    @Size(max = 200, message = "채널 설명은 200자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "채널 타입은 필수입니다")
    private ChannelType channelType;

    private String password; // 비공개 채널용
    private Boolean requiresApproval;
    private Integer maxMembers;
    private String topic;
    private List<String> tags;
    private List<String> initialMemberIds; // 초기 멤버들
}