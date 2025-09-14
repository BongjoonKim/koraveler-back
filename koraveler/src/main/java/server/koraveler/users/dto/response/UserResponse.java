
// UserResponse.java
package server.koraveler.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private String id;           // MongoDB _id
    private String userId;       // 사용자 ID (username)
    private String name;         // 사용자 이름
    private String email;        // 이메일
    private String profileImage; // 프로필 이미지 URL
    private String status;       // ACTIVE, INACTIVE
    private LocalDateTime createdAt;
}