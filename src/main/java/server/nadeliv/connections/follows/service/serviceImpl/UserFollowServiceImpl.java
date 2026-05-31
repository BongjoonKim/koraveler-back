package server.nadeliv.connections.follows.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.nadeliv.connections.follows.dto.FollowStatusDTO;
import server.nadeliv.connections.follows.dto.FollowUserDTO;
import server.nadeliv.connections.follows.model.UserFollow;
import server.nadeliv.connections.follows.repo.UserFollowsRepo;
import server.nadeliv.connections.follows.service.UserFollowService;
import server.nadeliv.users.model.Users;
import server.nadeliv.users.repo.UsersRepo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowsRepo userFollowsRepo;
    private final UsersRepo usersRepo;

    @Override
    public FollowStatusDTO follow(String followerId, String targetUserId) {
        validate(followerId, targetUserId);
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다");
        }

        if (!userFollowsRepo.existsByFollowerIdAndFollowingId(followerId, targetUserId)) {
            UserFollow entry = UserFollow.builder()
                    .followerId(followerId)
                    .followingId(targetUserId)
                    .build();
            entry.setCreated(LocalDateTime.now());
            entry.setUpdated(LocalDateTime.now());
            entry.setCreatedUser(followerId);
            entry.setUpdatedUser(followerId);
            try {
                userFollowsRepo.save(entry);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 동시 요청으로 인한 중복은 무시 (멱등성)
                log.debug("중복 follow 요청 무시: {} -> {}", followerId, targetUserId);
            }
        }

        return buildStatus(followerId, targetUserId, true);
    }

    @Override
    public FollowStatusDTO unfollow(String followerId, String targetUserId) {
        validate(followerId, targetUserId);
        userFollowsRepo.deleteByFollowerIdAndFollowingId(followerId, targetUserId);
        return buildStatus(followerId, targetUserId, false);
    }

    @Override
    public FollowStatusDTO getStatus(String viewerId, String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("대상 사용자가 필요합니다");
        }
        boolean isFollowing = viewerId != null
                && userFollowsRepo.existsByFollowerIdAndFollowingId(viewerId, targetUserId);
        return buildStatus(viewerId, targetUserId, isFollowing);
    }

    @Override
    public List<FollowUserDTO> getFollowing(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) return Collections.emptyList();
        List<UserFollow> follows = userFollowsRepo.findByFollowerId(viewerId);
        List<String> targetIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
        return hydrateUsers(targetIds);
    }

    @Override
    public List<FollowUserDTO> getFollowers(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) return Collections.emptyList();
        List<UserFollow> follows = userFollowsRepo.findByFollowingId(targetUserId);
        List<String> followerIds = follows.stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
        return hydrateUsers(followerIds);
    }

    @Override
    public List<String> getFollowingUserIds(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) return Collections.emptyList();
        return userFollowsRepo.findByFollowerId(viewerId).stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());
    }

    private void validate(String followerId, String targetUserId) {
        if (followerId == null || followerId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다");
        }
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("대상 사용자가 필요합니다");
        }
    }

    private FollowStatusDTO buildStatus(String viewerId, String targetUserId, boolean isFollowing) {
        long followerCount = userFollowsRepo.countByFollowingId(targetUserId);
        long followingCount = userFollowsRepo.countByFollowerId(targetUserId);
        return FollowStatusDTO.builder()
                .targetUserId(targetUserId)
                .isFollowing(isFollowing)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    private List<FollowUserDTO> hydrateUsers(List<String> userIds) {
        if (userIds.isEmpty()) return Collections.emptyList();
        // N+1 방지: findByUserIdIn 으로 한 번에 조회
        List<Users> users = usersRepo.findByUserIdIn(userIds);
        Map<String, Users> byUserId = users.stream()
                .collect(Collectors.toMap(Users::getUserId, u -> u, (a, b) -> a));
        return userIds.stream()
                .map(byUserId::get)
                .filter(java.util.Objects::nonNull)
                .map(u -> FollowUserDTO.builder()
                        .userId(u.getUserId())
                        .name(u.getName())
                        .src(u.getSrc())
                        .build())
                .collect(Collectors.toList());
    }
}
