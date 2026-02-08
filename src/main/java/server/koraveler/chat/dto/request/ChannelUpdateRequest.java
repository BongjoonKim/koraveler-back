package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelUpdateRequest {
    @Size(min = 2, max = 50, message = "채널명은 2-50자 사이여야 합니다")
    private String name;

    @Size(max = 200, message = "채널 설명은 200자를 초과할 수 없습니다")
    private String description;

    private String topic;
    private List<String> tags;
    private String avatarUrl;
    private Boolean isReadOnly;
    private Integer maxMembers;
}