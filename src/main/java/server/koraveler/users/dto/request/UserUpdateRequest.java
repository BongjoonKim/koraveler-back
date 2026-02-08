package server.koraveler.users.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String name;
    private String email;
    private String src;
    private LocalDateTime birthday;
}
