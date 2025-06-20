// 2. ChannelServiceImpl.java
package server.koraveler.chat.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import server.koraveler.chat.dto.mapper.ChannelMapper;
import server.koraveler.chat.model.entities.Channels;
import server.koraveler.chat.model.entities.ChannelMembers;
import server.koraveler.chat.model.enums.ChannelType;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.repository.ChannelsRepo;
import server.koraveler.chat.repository.ChannelMembersRepo;
import server.koraveler.chat.service.ChannelService;
import server.koraveler.chat.service.ChannelMemberService;
import server.koraveler.chat.exception.CustomException;
import server.koraveler.chat.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChannelServiceImpl implements ChannelService {

    private final ChannelsRepo channelsRepo;
    private final ChannelMembersRepo channelMembersRepo;
    private final ChannelMemberService channelMemberService;
    private final ChannelMapper channelMapper;

    @Override
    public ChannelResponse createChannel(ChannelCreateRequest request, String userId) {
        log.info("Creating channel: {} by user: {}", request.getName(), userId);

        // 채널명 중복 체크 (같은 타입에서)
        if (channelsRepo.existsByNameAndChannelType(request.getName(), request.getChannelType())) {
            throw new CustomException(ErrorCode.DUPLICATE_CHANNEL_NAME);
        }

        // 채널 생성
        Channels channel = channelMapper.toEntity(request, userId);
        Channels savedChannel = channelsRepo.save(channel);

        // 생성자를 채널 멤버로 추가
        channelMemberService.addMember(savedChannel.getId(), userId, userId);

        // 초기 멤버들 추가 (있는 경우)
        if (request.getInitialMemberIds() != null) {
            for (String memberId : request.getInitialMemberIds()) {
                if (!memberId.equals(userId)) {
                    channelMemberService.addMember(savedChannel.getId(), memberId, userId);
                }
            }
        }

        log.info("Channel created successfully: {}", savedChannel.getId());
        return channelMapper.toResponse(savedChannel);
    }

    @Override
    public ChannelResponse updateChannel(String channelId, ChannelUpdateRequest request, String userId) {
        log.info("Updating channel: {} by user: {}", channelId, userId);

        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 수정 권한 확인
        if (!hasChannelUpdatePermission(channelId, userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHANNEL_UPDATE);
        }

        // 채널 정보 업데이트
        if (request.getName() != null) {
            channel.setName(request.getName());
        }
        if (request.getDescription() != null) {
            channel.setDescription(request.getDescription());
        }
        if (request.getTopic() != null) {
            channel.setTopic(request.getTopic());
        }
        if (request.getTags() != null) {
            channel.setTags(request.getTags());
        }
        if (request.getAvatarUrl() != null) {
            channel.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getIsReadOnly() != null) {
            channel.setIsReadOnly(request.getIsReadOnly());
        }
        if (request.getMaxMembers() != null) {
            channel.setMaxMembers(request.getMaxMembers());
        }

        channel.setUpdatedUserId(userId);
        channel.setUpdatedAt(LocalDateTime.now());

        Channels updatedChannel = channelsRepo.save(channel);

        log.info("Channel updated successfully: {}", channelId);
        return channelMapper.toResponse(updatedChannel);
    }

    @Override
    public void deleteChannel(String channelId, String userId) {
        log.info("Deleting channel: {} by user: {}", channelId, userId);

        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 삭제 권한 확인
        if (!hasChannelDeletePermission(channelId, userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHANNEL_DELETE);
        }

        // 소프트 삭제 (아카이브)
        archiveChannel(channelId, userId);

        log.info("Channel deleted successfully: {}", channelId);
    }

    @Override
    public void archiveChannel(String channelId, String userId) {
        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        channel.setIsArchived(true);
        channel.setArchivedAt(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());

        channelsRepo.save(channel);
    }

    @Override
    public void unarchiveChannel(String channelId, String userId) {
        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        channel.setIsArchived(false);
        channel.setArchivedAt(null);
        channel.setUpdatedAt(LocalDateTime.now());

        channelsRepo.save(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponse getChannel(String channelId, String userId) {
        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 채널 접근 권한 확인
        validateChannelAccess(channelId, userId);

        ChannelResponse response = channelMapper.toResponse(channel);

        // 사용자별 추가 정보 설정
        enrichChannelResponse(response, userId);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelListResponse getUserChannels(String userId, PageRequest pageRequest) {
        // 사용자가 참여한 채널 ID 목록을 먼저 조회
        List<String> userChannelIds = getUserChannelIds(userId);

        // Pageable 객체 생성
        Pageable pageable = createPageable(pageRequest);

        List<Channels> channels = channelsRepo.findUserChannelsByIds(userId, userChannelIds, pageable);

        List<ChannelResponse> channelResponses = channels.stream()
                .map(channel -> {
                    ChannelResponse response = channelMapper.toResponse(channel);
                    enrichChannelResponse(response, userId);
                    return response;
                })
                .toList();

        return ChannelListResponse.builder()
                .channels(channelResponses)
                .totalCount(Math.toIntExact(channelsRepo.countUserChannelsByIds(userId, userChannelIds)))
                .hasNext(channels.size() == pageRequest.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelListResponse getPublicChannels(PageRequest pageRequest) {
        Pageable pageable = createPageable(pageRequest);
        List<Channels> channels = channelsRepo.findPublicChannels(pageable);

        List<ChannelResponse> channelResponses = channels.stream()
                .map(channelMapper::toResponse)
                .toList();

        return ChannelListResponse.builder()
                .channels(channelResponses)
                .totalCount(Math.toIntExact(channelsRepo.countPublicChannels()))
                .hasNext(channels.size() == pageRequest.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelListResponse searchChannels(String keyword, String userId, PageRequest pageRequest) {
        Pageable pageable = createPageable(pageRequest);

        // 공개 채널 검색
        List<Channels> publicChannels = channelsRepo.searchPublicChannels(keyword, pageable);

        // 사용자 채널 검색
        List<String> userChannelIds = getUserChannelIds(userId);
        List<Channels> userChannels = channelsRepo.searchUserChannels(keyword, userChannelIds, pageable);

        // 중복 제거하여 합치기
        List<Channels> allChannels = mergeChannelLists(publicChannels, userChannels, pageRequest.getSize());

        List<ChannelResponse> channelResponses = allChannels.stream()
                .map(channel -> {
                    ChannelResponse response = channelMapper.toResponse(channel);
                    enrichChannelResponse(response, userId);
                    return response;
                })
                .toList();

        // 전체 검색 결과 수 계산
        Long publicCount = channelsRepo.countSearchPublicChannels(keyword);
        Long userCount = channelsRepo.countSearchUserChannels(keyword, userChannelIds);
        Integer totalCount = Math.toIntExact(publicCount + userCount);

        return ChannelListResponse.builder()
                .channels(channelResponses)
                .totalCount(totalCount)
                .hasNext(allChannels.size() == pageRequest.getSize())
                .build();
    }

    @Override
    public ChannelResponse joinChannel(String channelId, ChannelJoinRequest request, String userId) {
        log.info("User {} joining channel: {}", userId, channelId);

        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 이미 멤버인지 확인
        if (channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new CustomException(ErrorCode.ALREADY_CHANNEL_MEMBER);
        }

        // 채널 타입별 참여 검증
        validateChannelJoin(channel, request, userId);

        // 멤버 추가
        channelMemberService.addMember(channelId, userId, userId);

        // 멤버 수 업데이트
        channel.setMemberCount(channel.getMemberCount() + 1);
        channelsRepo.save(channel);

        log.info("User {} joined channel {} successfully", userId, channelId);

        ChannelResponse response = channelMapper.toResponse(channel);
        enrichChannelResponse(response, userId);
        return response;
    }

    @Override
    public void leaveChannel(String channelId, String userId) {
        log.info("User {} leaving channel: {}", userId, channelId);

        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 채널 멤버인지 확인
        if (!channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new CustomException(ErrorCode.NOT_CHANNEL_MEMBER);
        }

        // 멤버 제거
        channelMemberService.removeMember(channelId, userId, userId);

        // 멤버 수 업데이트
        channel.setMemberCount(Math.max(0, channel.getMemberCount() - 1));
        channelsRepo.save(channel);

        log.info("User {} left channel {} successfully", userId, channelId);
    }

    @Override
    public ChannelResponse updateChannelSettings(String channelId, ChannelSettingsRequest request, String userId) {
        log.info("Updating channel settings for channel: {} by user: {}", channelId, userId);

        // 권한 확인
        if (!hasChannelUpdatePermission(channelId, userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_CHANNEL_UPDATE);
        }

        // 채널 설정 업데이트 로직
        // 실제 구현에서는 ChannelSettings 엔티티 사용하여 상세 설정 업데이트
        // 예: 알림 설정, 권한 설정, 자동 삭제 설정 등

        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 설정 업데이트 (request에 따라 구현)
        // if (request.getDefaultNotificationSettings() != null) {
        //     channel.setDefaultNotificationSettings(request.getDefaultNotificationSettings());
        // }

        channel.setUpdatedAt(LocalDateTime.now());
        channelsRepo.save(channel);

        return getChannel(channelId, userId);
    }

    @Override
    public String generateInviteLink(String channelId, String userId) {
        log.info("Generating invite link for channel: {} by user: {}", channelId, userId);

        // 권한 확인
        if (!hasInvitePermission(channelId, userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_INVITE);
        }

        // 초대 코드 생성
        String inviteCode = UUID.randomUUID().toString().substring(0, 8);

        // 초대 정보 저장 (ChannelInvitations 테이블)
        // 실제 구현에서는 초대 정보를 별도 테이블에 저장
        // ChannelInvitation invitation = ChannelInvitation.builder()
        //     .channelId(channelId)
        //     .inviteCode(inviteCode)
        //     .createdByUserId(userId)
        //     .expiresAt(LocalDateTime.now().plusDays(7)) // 7일 후 만료
        //     .build();
        // channelInvitationRepo.save(invitation);

        log.info("Invite link generated successfully for channel: {}", channelId);
        return inviteCode;
    }

    @Override
    public ChannelResponse joinByInviteLink(String inviteCode, String userId) {
        log.info("User {} joining channel by invite code: {}", userId, inviteCode);

        // 초대 링크로 채널 참여
        // ChannelInvitations에서 초대 코드 검증
        String channelId = findChannelByInviteCode(inviteCode);

        if (channelId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "유효하지 않은 초대 코드입니다");
        }

        ChannelJoinRequest request = new ChannelJoinRequest();
        request.setInviteCode(inviteCode);

        return joinChannel(channelId, request, userId);
    }

    // ===== Private Helper Methods =====

    private void validateChannelAccess(String channelId, String userId) {
        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 공개 채널이거나 멤버인 경우 접근 가능
        if (channel.getChannelType() == ChannelType.PUBLIC ||
                channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            return;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED_CHANNEL_ACCESS);
    }

    private void validateChannelJoin(Channels channel, ChannelJoinRequest request, String userId) {
        // 아카이브된 채널 체크
        if (channel.getIsArchived()) {
            throw new CustomException(ErrorCode.ARCHIVED_CHANNEL);
        }

        // 최대 멤버 수 체크
        if (channel.getMaxMembers() != null &&
                channel.getMemberCount() >= channel.getMaxMembers()) {
            throw new CustomException(ErrorCode.CHANNEL_FULL);
        }

        // 비공개 채널 비밀번호 체크
        if (channel.getChannelType() == ChannelType.PRIVATE &&
                channel.getPassword() != null) {
            if (request.getPassword() == null ||
                    !verifyPassword(request.getPassword(), channel.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_CHANNEL_PASSWORD);
            }
        }

        // 승인 필요한 채널 체크
        if (channel.getRequiresApproval() != null && channel.getRequiresApproval()) {
            // 승인 요청 로직
            throw new CustomException(ErrorCode.CHANNEL_APPROVAL_REQUIRED);
        }
    }

    private void enrichChannelResponse(ChannelResponse response, String userId) {
        // 사용자별 추가 정보 설정
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(
                response.getId(), userId).orElse(null);

        if (member != null) {
            response.setIsMember(true);
            response.setIsMuted(member.getIsMuted());
            response.setUnreadMessageCount(calculateUnreadCount(response.getId(), userId));
            // 역할 정보 설정
            // response.setMyRole(getUserRole(response.getId(), userId));
        } else {
            response.setIsMember(false);
            response.setIsMuted(false);
            response.setUnreadMessageCount(0);
        }
    }

    private Integer calculateUnreadCount(String channelId, String userId) {
        // 안 읽은 메시지 수 계산
        // MessagesRepository를 통해 계산
        // return messagesRepo.countUnreadMessages(channelId, userId);
        return 0; // 임시
    }

    private boolean hasChannelUpdatePermission(String channelId, String userId) {
        // 채널 업데이트 권한 확인
        // 채널 생성자이거나 관리자인지 확인
        Channels channel = channelsRepo.findById(channelId).orElse(null);
        if (channel != null && channel.getCreatedUserId().equals(userId)) {
            return true;
        }
        // 또는 ChannelMemberService를 통해 권한 확인
        // return channelMemberService.hasPermission(channelId, userId, "UPDATE_CHANNEL");
        return true; // 임시
    }

    private boolean hasChannelDeletePermission(String channelId, String userId) {
        // 채널 삭제 권한 확인
        // 채널 생성자이거나 관리자인지 확인
        Channels channel = channelsRepo.findById(channelId).orElse(null);
        if (channel != null && channel.getCreatedUserId().equals(userId)) {
            return true;
        }
        // return channelMemberService.hasPermission(channelId, userId, "DELETE_CHANNEL");
        return true; // 임시
    }

    private boolean hasInvitePermission(String channelId, String userId) {
        // 초대 권한 확인
        // 채널 멤버인지 확인 후 초대 권한 체크
        if (!channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            return false;
        }
        // return channelMemberService.hasPermission(channelId, userId, "INVITE_MEMBER");
        return true; // 임시
    }

    private boolean verifyPassword(String inputPassword, String storedPassword) {
        // 비밀번호 검증 (암호화된 비밀번호와 비교)
        // 실제 구현에서는 BCrypt 등을 사용
        // return passwordEncoder.matches(inputPassword, storedPassword);
        return inputPassword.equals(storedPassword); // 임시 (실제로는 암호화된 비밀번호 비교)
    }

    private String findChannelByInviteCode(String inviteCode) {
        // 초대 코드로 채널 ID 찾기
        // 실제 구현에서는 ChannelInvitations 테이블에서 조회
        // ChannelInvitation invitation = channelInvitationRepo.findByInviteCodeAndExpiresAtAfter(
        //     inviteCode, LocalDateTime.now()).orElse(null);
        // return invitation != null ? invitation.getChannelId() : null;
        return "channel_id"; // 임시
    }

    private List<String> getUserChannelIds(String userId) {
        // ChannelMembersRepo를 통해 사용자가 참여한 채널 ID 목록 조회
        return channelMembersRepo.findByUserIdAndStatus(userId, MemberStatus.ACTIVE)
                .stream()
                .map(ChannelMembers::getChannelId)
                .toList();
    }

    private Pageable createPageable(PageRequest pageRequest) {
        Sort.Direction direction = "asc".equalsIgnoreCase(pageRequest.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, pageRequest.getSortBy());

        return org.springframework.data.domain.PageRequest.of(0, pageRequest.getSize(), sort);
    }

    private List<Channels> mergeChannelLists(List<Channels> publicChannels, List<Channels> userChannels, Integer maxSize) {
        // 중복 제거하여 합치기
        Set<String> addedIds = new HashSet<>();
        List<Channels> result = new ArrayList<>();

        // 공개 채널 먼저 추가
        for (Channels channel : publicChannels) {
            if (result.size() >= maxSize) break;
            if (!addedIds.contains(channel.getId())) {
                result.add(channel);
                addedIds.add(channel.getId());
            }
        }

        // 사용자 채널 추가 (중복 제외)
        for (Channels channel : userChannels) {
            if (result.size() >= maxSize) break;
            if (!addedIds.contains(channel.getId())) {
                result.add(channel);
                addedIds.add(channel.getId());
            }
        }

        return result;
    }
}