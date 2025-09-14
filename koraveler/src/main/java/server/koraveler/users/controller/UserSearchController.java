// UserSearchController.java
package server.koraveler.users.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.users.dto.CustomUserDetails;
import server.koraveler.users.dto.response.UserResponse;
import server.koraveler.users.dto.response.UserSearchResponse;
import server.koraveler.users.service.UserSearchService;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")  // ps 제거 - 토큰 필요
public class UserSearchController {

    private final UserSearchService userSearchService;

    /**
     * 사용자 검색 (키워드로)
     */
    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String excludeChannelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Searching users with keyword: {} by user: {}", keyword, userDetails.getUsername());

        UserSearchResponse response = userSearchService.searchUsers(
                keyword, excludeChannelId, size, userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * 채널에 없는 사용자 검색
     */
    @GetMapping("/search/available")
    public ResponseEntity<UserSearchResponse> searchUsersNotInChannel(
            @RequestParam String keyword,
            @RequestParam String channelId,
            @RequestParam(defaultValue = "20") Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Searching available users for channel: {} with keyword: {}", channelId, keyword);

        UserSearchResponse response = userSearchService.searchUsersNotInChannel(
                keyword, channelId, size, userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자 정보 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Getting user info for userId: {} by user: {}", userId, userDetails.getUsername());

        UserResponse response = userSearchService.getUserById(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 목록 조회 (페이징)
     */
    @GetMapping("")
    public ResponseEntity<UserSearchResponse> getUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Getting user list page: {} size: {}", page, size);

        UserSearchResponse response = userSearchService.getUsers(page, size, sortBy, sortDirection);

        return ResponseEntity.ok(response);
    }

    /**
     * 현재 로그인한 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Getting current user info: {}", userDetails.getUsername());

        UserResponse response = userSearchService.getUserById(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자명으로 사용자 검색 (정확히 일치)
     */
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Getting user by username: {}", username);

        UserResponse response = userSearchService.getUserByUsername(username);

        return ResponseEntity.ok(response);
    }
}