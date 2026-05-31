package server.nadeliv.connections.follows.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FollowStatusDTO {
    private String targetUserId;
    private boolean isFollowing;
    private long followerCount;   // 대상의 팔로워 수
    private long followingCount;  // 대상이 팔로우 중인 수
}
