package server.nadeliv.users.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 헤더/사이드바에 표시할 사용자별 뱃지 카운트 통합 응답.
 * 향후 메신저 unread, 알림 count 등을 추가할 수 있도록 확장 가능 구조.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserBadgesResponse {
    // Following 피드의 마지막 확인 시점 이후 등록된 새 글 수
    private long followingUnread;

    // 클라이언트가 since 로 보낸 값을 그대로 반환 (디버그/검증용)
    private LocalDateTime since;

    // 응답 생성 시점
    private LocalDateTime generatedAt;
}
