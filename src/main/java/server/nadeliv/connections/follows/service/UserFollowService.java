package server.nadeliv.connections.follows.service;

import server.nadeliv.connections.follows.dto.FollowStatusDTO;
import server.nadeliv.connections.follows.dto.FollowUserDTO;

import java.util.List;

public interface UserFollowService {

    // 현재 인증된 사용자가 targetUserId 를 팔로우. 멱등적으로 동작.
    FollowStatusDTO follow(String followerId, String targetUserId);

    // 언팔로우. 멱등적으로 동작.
    FollowStatusDTO unfollow(String followerId, String targetUserId);

    // 단순 상태 조회 (비인증 호출 가능)
    FollowStatusDTO getStatus(String viewerId, String targetUserId);

    // viewerId 가 팔로우하고 있는 사용자 목록
    List<FollowUserDTO> getFollowing(String viewerId);

    // targetUserId 의 팔로워 목록
    List<FollowUserDTO> getFollowers(String targetUserId);

    // viewerId 가 팔로우 중인 사용자들의 userId 목록 (피드 쿼리에 사용)
    List<String> getFollowingUserIds(String viewerId);
}
