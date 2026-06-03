package server.nadeliv.users.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.blog.service.BlogService;
import server.nadeliv.users.dto.CustomUserDetails;
import server.nadeliv.users.dto.request.PasswordChangeRequest;
import server.nadeliv.users.dto.request.UserDeleteRequest;
import server.nadeliv.users.dto.request.UserUpdateRequest;
import server.nadeliv.users.dto.response.UserBadgesResponse;
import server.nadeliv.users.dto.response.UserProfileResponse;
import server.nadeliv.users.service.UsersService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/user")
@Slf4j
public class UsersController {

    @Autowired
    private UsersService userService;

    @Autowired
    private BlogService blogService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserUpdateRequest request
    ) {
        UserProfileResponse profile = userService.updateUserProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PasswordChangeRequest request
    ) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserDeleteRequest request
    ) {
        userService.deleteUser(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 사용자의 사이드바/헤더용 뱃지 카운트 통합 조회.
     * @param followingSince 클라이언트가 마지막으로 Following 탭을 확인한 시각 (ISO-8601).
     *                       미지정 시 최근 7일을 기본으로 사용.
     */
    @GetMapping("/me/badges")
    public ResponseEntity<?> getMyBadges(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "followingSince", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime followingSince
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        LocalDateTime since = followingSince != null
                ? followingSince
                : LocalDateTime.now().minus(7, ChronoUnit.DAYS);

        long followingUnread = blogService.countFollowingFeedSince(userDetails.getUsername(), since);

        UserBadgesResponse response = UserBadgesResponse.builder()
                .followingUnread(followingUnread)
                .since(since)
                .generatedAt(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
