package server.koraveler.common.dto;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@Setter
//@AllArgsConstructor
//@NoArgsConstructor
public class Common {
    private LocalDateTime created;
    private LocalDateTime updated;
    private boolean isNonExpired;
    private boolean isNonLocked;
    private boolean isCredentialsNonExpired;
    private boolean isEnabled;
}
