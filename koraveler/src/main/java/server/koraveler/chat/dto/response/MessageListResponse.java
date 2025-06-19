// 2. MessageListResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageListResponse {
    private List<MessageResponse> messages;
    private Boolean hasNext; // 다음 페이지 존재 여부
    private String nextCursor; // 다음 페이지 커서
    private Integer totalCount; // 전체 메시지 수
}