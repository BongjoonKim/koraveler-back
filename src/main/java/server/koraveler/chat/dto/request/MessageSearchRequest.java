// MessageSearchRequest.java
package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.chat.model.enums.MessageType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageSearchRequest extends PageRequest {
    private String keyword; // 검색 키워드
    private String userId; // 특정 사용자 메시지만
    private MessageType messageType; // 특정 타입만
    private LocalDateTime startDate; // 날짜 범위
    private LocalDateTime endDate;
    private Boolean hasAttachments; // 첨부파일 있는 메시지만
    private Boolean isPinned; // 고정 메시지만
}