package server.koraveler.common.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserCommon {
    private LocalDateTime created;
    private LocalDateTime updated;

    // 기본값을 true로 초기화
    private boolean isNonExpired = true;
    private boolean isNonLocked = true;
    private boolean isCredentialsNonExpired = true;
    private boolean isEnabled = true;
}