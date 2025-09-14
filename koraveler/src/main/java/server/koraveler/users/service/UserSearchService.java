// UserSearchService.java
package server.koraveler.users.service;

import server.koraveler.users.dto.response.UserResponse;
import server.koraveler.users.dto.response.UserSearchResponse;

public interface UserSearchService {

    /**
     * 사용자 검색 (키워드로)
     * @param keyword 검색 키워드
     * @param excludeChannelId 제외할 채널 ID (해당 채널의 멤버는 제외)
     * @param size 결과 개수
     * @param currentUserId 현재 사용자 ID
     * @return 검색 결과
     */
    UserSearchResponse searchUsers(String keyword, String excludeChannelId, Integer size, String currentUserId);

    /**
     * 채널에 없는 사용자 검색
     * @param keyword 검색 키워드
     * @param channelId 채널 ID
     * @param size 결과 개수
     * @param currentUserId 현재 사용자 ID
     * @return 검색 결과
     */
    UserSearchResponse searchUsersNotInChannel(String keyword, String channelId, Integer size, String currentUserId);

    /**
     * 특정 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    UserResponse getUserById(String userId);

    /**
     * 사용자명으로 사용자 조회
     * @param username 사용자명
     * @return 사용자 정보
     */
    UserResponse getUserByUsername(String username);

    /**
     * 사용자 목록 조회 (페이징)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sortBy 정렬 기준
     * @param sortDirection 정렬 방향
     * @return 사용자 목록
     */
    UserSearchResponse getUsers(Integer page, Integer size, String sortBy, String sortDirection);
}