// AdminUserController.java
package server.nadeliv.users.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.users.dto.CustomUserDetails;
import server.nadeliv.users.dto.request.AdminRoleUpdateRequest;
import server.nadeliv.users.dto.request.AdminStatusUpdateRequest;
import server.nadeliv.users.dto.response.AdminUserDetailResponse;
import server.nadeliv.users.dto.response.AdminUserListResponse;
import server.nadeliv.users.dto.response.AdminUserSummaryResponse;
import server.nadeliv.users.service.AdminUserService;

/**
 * 관리자 전용 사용자 관리 API.
 * URL 매처와 별개로 @PreAuthorize 가 admin 권한을 서버측에서 강제한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAuthority('admin')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 가입자 목록 (검색·상태 필터·페이징)
     */
    @GetMapping("")
    public ResponseEntity<AdminUserListResponse> getUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status) {

        return ResponseEntity.ok(adminUserService.getUsers(page, size, keyword, status));
    }

    /**
     * 사용자 상세 — 기본 정보 + 작성한 글(페이징) + 여행 프로젝트
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") Integer docPage,
            @RequestParam(defaultValue = "10") Integer docSize) {

        return ResponseEntity.ok(adminUserService.getUserDetail(userId, docPage, docSize));
    }

    /**
     * 권한 변경
     */
    @PutMapping("/{userId}/roles")
    public ResponseEntity<AdminUserSummaryResponse> updateRoles(
            @PathVariable String userId,
            @RequestBody AdminRoleUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                adminUserService.updateRoles(userId, request, userDetails.getUsername()));
    }

    /**
     * 탈퇴 처리 / 계정 복구
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserSummaryResponse> updateStatus(
            @PathVariable String userId,
            @RequestBody AdminStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                adminUserService.updateStatus(userId, request, userDetails.getUsername()));
    }
}
