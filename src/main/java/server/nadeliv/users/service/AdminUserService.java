// AdminUserService.java
package server.nadeliv.users.service;

import server.nadeliv.users.dto.request.AdminRoleUpdateRequest;
import server.nadeliv.users.dto.request.AdminStatusUpdateRequest;
import server.nadeliv.users.dto.response.AdminUserDetailResponse;
import server.nadeliv.users.dto.response.AdminUserListResponse;
import server.nadeliv.users.dto.response.AdminUserSummaryResponse;

public interface AdminUserService {

    /**
     * 가입자 목록 조회 (검색·상태 필터·페이징)
     * @param status all | active | disabled
     */
    AdminUserListResponse getUsers(int page, int size, String keyword, String status);

    /**
     * 사용자 상세 — 기본 정보 + 작성한 글(페이징) + 참여 여행 프로젝트
     */
    AdminUserDetailResponse getUserDetail(String userId, int docPage, int docSize);

    /**
     * 권한 변경 (자기 자신은 불가)
     */
    AdminUserSummaryResponse updateRoles(String targetUserId, AdminRoleUpdateRequest request, String adminUserId);

    /**
     * 탈퇴 처리(enabled=false) / 복구(enabled=true). 자기 자신은 불가.
     */
    AdminUserSummaryResponse updateStatus(String targetUserId, AdminStatusUpdateRequest request, String adminUserId);
}
