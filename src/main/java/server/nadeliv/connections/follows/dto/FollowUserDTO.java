package server.nadeliv.connections.follows.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팔로우/팔로잉 목록에서 사용자 한 명을 표시하기 위한 경량 DTO.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FollowUserDTO {
    private String userId;
    private String name;
    private String src;  // 아바타 이미지 URL
}
