package server.nadeliv.connections.follows.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.connections.follows.dto.FollowStatusDTO;
import server.nadeliv.connections.follows.dto.FollowUserDTO;
import server.nadeliv.connections.follows.service.UserFollowService;
import server.nadeliv.users.dto.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
@Slf4j
public class UserFollowController {

    private final UserFollowService userFollowService;

    /**
     * targetUserId 를 팔로우 (인증 필요).
     */
    @PostMapping("/{targetUserId}")
    public ResponseEntity<?> follow(
            @PathVariable String targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        try {
            FollowStatusDTO status = userFollowService.follow(userDetails.getUsername(), targetUserId);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("팔로우 실패: target={}", targetUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("팔로우 실패");
        }
    }

    /**
     * targetUserId 를 언팔로우 (인증 필요).
     */
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<?> unfollow(
            @PathVariable String targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        try {
            FollowStatusDTO status = userFollowService.unfollow(userDetails.getUsername(), targetUserId);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("언팔로우 실패: target={}", targetUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("언팔로우 실패");
        }
    }

    /**
     * 팔로우 상태 + 카운트 조회 (비인증 허용).
     */
    @GetMapping("/ps/{targetUserId}/status")
    public ResponseEntity<?> status(
            @PathVariable String targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String viewerId = userDetails != null ? userDetails.getUsername() : null;
            FollowStatusDTO status = userFollowService.getStatus(viewerId, targetUserId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("팔로우 상태 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("조회 실패");
        }
    }

    /**
     * 내가 팔로우 중인 사용자 목록 (인증 필요).
     */
    @GetMapping("/me/following")
    public ResponseEntity<?> myFollowing(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        List<FollowUserDTO> users = userFollowService.getFollowing(userDetails.getUsername());
        return ResponseEntity.ok(users);
    }

    /**
     * 나의 팔로워 목록 (인증 필요).
     */
    @GetMapping("/me/followers")
    public ResponseEntity<?> myFollowers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        List<FollowUserDTO> users = userFollowService.getFollowers(userDetails.getUsername());
        return ResponseEntity.ok(users);
    }

    /**
     * 특정 사용자의 팔로워 목록 (비인증 허용 - 공개 프로필).
     */
    @GetMapping("/ps/{targetUserId}/followers")
    public ResponseEntity<?> userFollowers(@PathVariable String targetUserId) {
        return ResponseEntity.ok(userFollowService.getFollowers(targetUserId));
    }

    /**
     * 특정 사용자가 팔로우 중인 목록 (비인증 허용).
     */
    @GetMapping("/ps/{targetUserId}/following")
    public ResponseEntity<?> userFollowing(@PathVariable String targetUserId) {
        return ResponseEntity.ok(userFollowService.getFollowing(targetUserId));
    }
}
