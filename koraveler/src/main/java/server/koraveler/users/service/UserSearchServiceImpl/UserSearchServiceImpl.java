// UserSearchServiceImpl.java
package server.koraveler.users.service.UserSearchServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.koraveler.chat.model.entities.ChannelMembers;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.repository.ChannelMembersRepo;
import server.koraveler.error.CustomException;
import server.koraveler.users.dto.response.UserResponse;
import server.koraveler.users.dto.response.UserSearchResponse;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.UserSearchService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchServiceImpl implements UserSearchService {

    private final UsersRepo usersRepo;
    private final ChannelMembersRepo channelMembersRepo;

    @Override
    public UserSearchResponse searchUsers(String keyword, String excludeChannelId, Integer size, String currentUserId) {
        log.info("Searching users with keyword: {}", keyword);

        // 키워드가 너무 짧으면 빈 결과 반환
        if (keyword == null || keyword.trim().length() < 2) {
            return UserSearchResponse.builder()
                    .users(new ArrayList<>())
                    .totalCount(0)
                    .hasNext(false)
                    .build();
        }

        String trimmedKeyword = keyword.trim();

        // 제외할 사용자 ID 목록 가져오기
        Set<String> excludeUserIds = getExcludeUserIds(excludeChannelId);

        // 현재 사용자도 제외
        excludeUserIds.add(currentUserId);

        // 페이징 설정
        Pageable pageable = PageRequest.of(0, size + 1);

        // 사용자 검색 (userId, name, email로 검색) - Spring Data 메서드 사용
        Page<Users> searchPage = usersRepo.findByIsEnabledTrueAndUserIdContainingIgnoreCaseOrIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndEmailContainingIgnoreCase(
                trimmedKeyword, trimmedKeyword, trimmedKeyword, pageable);

        List<Users> searchResults = searchPage.getContent();

        // 제외할 사용자 필터링
        List<Users> filteredUsers = searchResults.stream()
                .filter(user -> !excludeUserIds.contains(user.getUserId()))
                .limit(size)
                .collect(Collectors.toList());

        // Response 변환
        List<UserResponse> userResponses = filteredUsers.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        boolean hasNext = searchResults.size() > size;

        return UserSearchResponse.builder()
                .users(userResponses)
                .totalCount(userResponses.size())
                .hasNext(hasNext)
                .build();
    }

    @Override
    public UserSearchResponse searchUsersNotInChannel(String keyword, String channelId, Integer size, String currentUserId) {
        log.info("Searching users not in channel: {} with keyword: {}", channelId, keyword);

        // 키워드가 너무 짧으면 빈 결과 반환
        if (keyword == null || keyword.trim().length() < 2) {
            return UserSearchResponse.builder()
                    .users(new ArrayList<>())
                    .totalCount(0)
                    .hasNext(false)
                    .build();
        }

        String trimmedKeyword = keyword.trim();

        // 채널의 현재 멤버 목록 가져오기
        List<ChannelMembers> channelMembers = channelMembersRepo.findByChannelIdAndStatus(
                channelId, MemberStatus.ACTIVE);

        List<String> memberUserIds = channelMembers.stream()
                .map(ChannelMembers::getUserId)
                .collect(Collectors.toList());

        // 현재 사용자도 제외 리스트에 추가
        if (!memberUserIds.contains(currentUserId)) {
            memberUserIds.add(currentUserId);
        }

        // 활성 사용자 중에서 검색 (Spring Data 메서드 사용)
        List<Users> searchResults;

        if (memberUserIds.isEmpty()) {
            // 제외할 멤버가 없는 경우 - 페이징 사용
            Pageable pageable = PageRequest.of(0, size);
            Page<Users> searchPage = usersRepo.findByIsEnabledTrueAndUserIdContainingIgnoreCaseOrIsEnabledTrueAndNameContainingIgnoreCaseOrIsEnabledTrueAndEmailContainingIgnoreCase(
                    trimmedKeyword, trimmedKeyword, trimmedKeyword, pageable);
            searchResults = searchPage.getContent();
        } else {
            // 채널 멤버를 제외하고 검색
            // 먼저 활성 사용자 중 채널 멤버가 아닌 사용자들을 조회
            List<Users> eligibleUsers = usersRepo.findByIsEnabledTrueAndUserIdNotIn(memberUserIds);

            // 키워드로 필터링
            searchResults = eligibleUsers.stream()
                    .filter(user ->
                            user.getUserId().toLowerCase().contains(trimmedKeyword.toLowerCase()) ||
                                    (user.getName() != null && user.getName().toLowerCase().contains(trimmedKeyword.toLowerCase())) ||
                                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(trimmedKeyword.toLowerCase()))
                    )
                    .limit(size)
                    .collect(Collectors.toList());
        }

        // Response 변환
        List<UserResponse> userResponses = searchResults.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return UserSearchResponse.builder()
                .users(userResponses)
                .totalCount(userResponses.size())
                .hasNext(false)
                .build();
    }

    @Override
    public UserResponse getUserById(String userId) {
        log.info("Getting user by ID: {}", userId);

        Users user = usersRepo.findByUserId(userId);
        if (user == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId);
        }

        return toUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);

        Users user = usersRepo.findByUserId(username);
        if (user == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + username);
        }

        return toUserResponse(user);
    }

    @Override
    public UserSearchResponse getUsers(Integer page, Integer size, String sortBy, String sortDirection) {
        log.info("Getting users - page: {}, size: {}", page, size);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Users> userPage = usersRepo.findByIsEnabledTrue(pageable);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return UserSearchResponse.builder()
                .users(userResponses)
                .totalCount((int) userPage.getTotalElements())
                .hasNext(userPage.hasNext())
                .currentPage(page)
                .totalPages(userPage.getTotalPages())
                .build();
    }

    /**
     * 제외할 사용자 ID 목록 가져오기
     */
    private Set<String> getExcludeUserIds(String excludeChannelId) {
        if (excludeChannelId == null || excludeChannelId.isEmpty()) {
            return Set.of();
        }

        List<ChannelMembers> members = channelMembersRepo.findByChannelIdAndStatus(
                excludeChannelId, MemberStatus.ACTIVE);

        return members.stream()
                .map(ChannelMembers::getUserId)
                .collect(Collectors.toSet());
    }

    /**
     * Users 엔티티를 UserResponse로 변환
     */
    private UserResponse toUserResponse(Users user) {
        return UserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImage(user.getSrc())
                .status(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                .createdAt(user.getCreated())
                .build();
    }
}