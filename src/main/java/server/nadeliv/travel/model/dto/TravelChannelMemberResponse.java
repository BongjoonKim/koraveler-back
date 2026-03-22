package server.nadeliv.travel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Travel 채널에 추가 가능한 멤버 정보 DTO
 * (프로젝트 멤버 중 아직 채널에 없는 사용자)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelChannelMemberResponse {
    private String userId;
    private String name;
    private String email;
    private String src; // 프로필 이미지
    private String travelRole; // ADMIN, USER, VIEWER
    private String nickname; // Travel 프로젝트 내 닉네임
}
