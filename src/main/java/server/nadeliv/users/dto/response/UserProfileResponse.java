package server.nadeliv.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponse {
    private String id;
    private String userId;
    private String email;
    private String name;
    private String src;
    private LocalDateTime birthday;
    private List<String> roles;
    private LocalDateTime created;
    private LocalDateTime updated;
}
