// 3. ChannelMemberServiceImpl.java
package server.nadeliv.chat.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.nadeliv.chat.dto.event.ChannelEvent;
import server.nadeliv.chat.dto.mapper.ChannelMapper;
import server.nadeliv.chat.dto.request.ChannelJoinRequest;
import server.nadeliv.chat.dto.response.ChannelMemberResponse;
import server.nadeliv.chat.dto.response.ChannelResponse;
import server.nadeliv.chat.model.entities.ChannelAuthorities;
import server.nadeliv.chat.model.entities.ChannelMembers;
import server.nadeliv.chat.model.entities.Channels;
import server.nadeliv.chat.model.enums.ChannelType;
import server.nadeliv.chat.model.enums.MemberStatus;
import server.nadeliv.chat.model.enums.NotificationLevel;
import server.nadeliv.chat.repository.ChannelMembersRepo;
import server.nadeliv.chat.repository.ChannelAuthoritiesRepo;
import server.nadeliv.chat.repository.ChannelsRepo;
import server.nadeliv.chat.service.ChannelMemberService;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChannelMemberServiceImpl implements ChannelMemberService {

    private final ChannelMembersRepo channelMembersRepo;
    private final ChannelsRepo channelsRepo;
    private final ChannelAuthoritiesRepo channelAuthoritiesRepo;
    private final ChannelMapper channelMapper;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChannelMemberResponse addMember(String channelId, String userId, String addedByUserId) {
        log.info("Adding member {} to channel {} by {}", userId, channelId, addedByUserId);

        // 멤버 추가 권한 확인 (자기 자신을 추가하는 경우는 권한 체크 생략)
        if (!addedByUserId.equals(userId) && !hasAddMemberPermission(channelId, addedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ADD_MEMBER);
        }

        // 기존 멤버십 확인
        Optional<ChannelMembers> existingMember = channelMembersRepo.findByChannelIdAndUserId(channelId, userId);

        ChannelMembers savedMember = null;

        if (existingMember.isPresent()) {
            ChannelMembers member = existingMember.get();

            // 이미 ACTIVE 상태면 에러
            if (member.getStatus() == MemberStatus.ACTIVE) {
                throw new CustomException(ErrorCode.ALREADY_CHANNEL_MEMBER);
            }

            // LEFT 상태면 다시 ACTIVE로 변경
            member.setStatus(MemberStatus.ACTIVE);
            member.setLastSeenAt(LocalDateTime.now());
            member.setUpdatedAt(LocalDateTime.now());
            savedMember = channelMembersRepo.save(member);

            log.info("Member {} rejoined channel {}", userId, channelId);

        } else {
            // 새 멤버 생성
            ChannelMembers member = ChannelMembers.builder()
                    .userId(userId)
                    .channelId(channelId)
                    .status(MemberStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .notificationsEnabled(true)
                    .notificationLevel(NotificationLevel.ALL)
                    .isMuted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            savedMember = channelMembersRepo.save(member);
            log.info("New member {} added to channel {}", userId, channelId);
        }



        // 새 멤버에게 기본 권한 부여 (이미 권한이 없는 경우에만)
        if (!channelAuthoritiesRepo.existsByChannelIdAndUserId(channelId, userId)) {
            // 채널 정보를 가져와서 생성자인지 확인
            Channels channel = channelsRepo.findById(channelId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

            boolean isOwner = channel.getCreatedUserId().equals(userId);

            ChannelAuthorities memberAuthority = ChannelAuthorities.builder()
                    .channelId(channelId)
                    .userId(userId)
                    .roleId(isOwner ? "OWNER" : "MEMBER")
                    .permissions(isOwner ?
                            List.of("ADD_MEMBER", "REMOVE_MEMBER", "UPDATE_MEMBER",
                                    "UPDATE_ROLE", "MUTE_MEMBER", "BAN_MEMBER",
                                    "DELETE_MESSAGE", "UPDATE_CHANNEL", "DELETE_CHANNEL") :
                            List.of("READ_MESSAGE", "SEND_MESSAGE", "LEAVE_CHANNEL"))
                    .grantedByUserId(addedByUserId)
                    .grantedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            channelAuthoritiesRepo.save(memberAuthority);
            log.info("Member authorities created for user: {} in channel: {}", userId, channelId);
        }

        log.info("Member {} added to channel {} successfully", userId, channelId);

        // 멤버 추가 이벤트 전송
        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId + "/member-added",
                ChannelEvent.builder()
                        .eventType("MEMBER_ADDED")
                        .channelId(channelId)
                        .userId(addedByUserId)
                        .data(Map.of(
                                "addedUserId", userId,
                                "addedByUserId", addedByUserId
                        ))
                        .build()
        );

        return toChannelMemberResponse(savedMember);
    }

    @Override
    public void removeMember(String channelId, String userId, String removedByUserId) {
        log.info("Removing member {} from channel {} by {}", userId, channelId, removedByUserId);

        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        // 멤버 제거 권한 확인
        if (!removedByUserId.equals(userId) && !hasRemoveMemberPermission(channelId, removedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_REMOVE_MEMBER);
        }

        // 멤버 상태 변경 (소프트 삭제)
        member.setStatus(MemberStatus.LEFT);
        member.setLeftAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        channelMembersRepo.save(member);

        // 멤버 제거 이벤트 전송
        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId + "/member-removed",
                ChannelEvent.builder()
                        .eventType("MEMBER_REMOVED")
                        .channelId(channelId)
                        .userId(removedByUserId)
                        .data(Map.of(
                                "removedUserId", userId,
                                "removedByUserId", removedByUserId
                        ))
                        .build()
        );

        log.info("Member {} removed from channel {} successfully", userId, channelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getChannelMembers(String channelId, String userId) {
        // 채널 접근 권한 확인
        validateChannelMembership(channelId, userId);

        List<ChannelMembers> members = channelMembersRepo.findByChannelIdAndStatus(
                channelId, MemberStatus.ACTIVE);

        return members.stream()
                .map(this::toChannelMemberResponse)
                .toList();
    }

    @Override
    public ChannelMemberResponse updateMemberStatus(String channelId, String targetUserId,
                                                    MemberStatus status, String updatedByUserId) {
        log.info("Updating member {} status to {} in channel {} by {}",
                targetUserId, status, channelId, updatedByUserId);

        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        // 상태 변경 권한 확인
        if (!hasUpdateMemberPermission(channelId, updatedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_UPDATE_MEMBER);
        }

        member.setStatus(status);
        member.setUpdatedAt(LocalDateTime.now());

        ChannelMembers updatedMember = channelMembersRepo.save(member);

        log.info("Member {} status updated to {} successfully", targetUserId, status);
        return toChannelMemberResponse(updatedMember);
    }

    @Override
    public ChannelMemberResponse updateMemberRole(String channelId, String targetUserId,
                                                  String roleId, String updatedByUserId) {
        log.info("Updating member {} role to {} in channel {} by {}",
                targetUserId, roleId, channelId, updatedByUserId);

        // 역할 변경 권한 확인
        if (!hasUpdateRolePermission(channelId, updatedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_UPDATE_ROLE);
        }

        // ChannelAuthorities 업데이트
        channelAuthoritiesRepo.updateMemberRole(channelId, targetUserId, roleId);

        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        log.info("Member {} role updated successfully", targetUserId);

        // 역할 변경 이벤트 전송
        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId + "/member-role-updated",
                ChannelEvent.builder()
                        .eventType("MEMBER_ROLE_UPDATED")
                        .channelId(channelId)
                        .userId(updatedByUserId)
                        .data(Map.of(
                                "targetUserId", targetUserId,
                                "newRoleId", roleId,
                                "updatedByUserId", updatedByUserId
                        ))
                        .build()
        );

        return toChannelMemberResponse(member);
    }

    @Override
    public ChannelMemberResponse updateMemberNickname(String channelId, String nickname, String userId) {
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        member.setNickname(nickname);
        member.setUpdatedAt(LocalDateTime.now());

        ChannelMembers updatedMember = channelMembersRepo.save(member);
        return toChannelMemberResponse(updatedMember);
    }

    @Override
    public ChannelMemberResponse updateNotificationSettings(String channelId, NotificationLevel level,
                                                            Boolean enabled, String userId) {
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        member.setNotificationLevel(level);
        member.setNotificationsEnabled(enabled);
        member.setUpdatedAt(LocalDateTime.now());

        ChannelMembers updatedMember = channelMembersRepo.save(member);
        return toChannelMemberResponse(updatedMember);
    }

    @Override
    public ChannelMemberResponse muteMember(String channelId, String targetUserId,
                                            Long muteMinutes, String mutedByUserId) {
        log.info("Muting member {} in channel {} for {} minutes by {}",
                targetUserId, channelId, muteMinutes, mutedByUserId);

        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        // 음소거 권한 확인
        if (!hasMutePermission(channelId, mutedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MUTE_MEMBER);
        }

        member.setIsMuted(true);
        member.setMutedUntil(LocalDateTime.now().plusMinutes(muteMinutes));
        member.setUpdatedAt(LocalDateTime.now());

        ChannelMembers mutedMember = channelMembersRepo.save(member);

        log.info("Member {} muted successfully", targetUserId);

        // 멤버 음소거 이벤트 전송
        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId + "/member-muted",
                ChannelEvent.builder()
                        .eventType("MEMBER_MUTED")
                        .channelId(channelId)
                        .userId(mutedByUserId)
                        .data(Map.of(
                                "mutedUserId", targetUserId,
                                "mutedByUserId", mutedByUserId,
                                "muteMinutes", muteMinutes
                        ))
                        .build()
        );

        return toChannelMemberResponse(mutedMember);
    }

    @Override
    public ChannelMemberResponse unmuteMember(String channelId, String targetUserId, String unmutedByUserId) {
        log.info("Unmuting member {} in channel {} by {}", targetUserId, channelId, unmutedByUserId);

        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        // 음소거 해제 권한 확인
        if (!hasMutePermission(channelId, unmutedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MUTE_MEMBER);
        }

        member.setIsMuted(false);
        member.setMutedUntil(null);
        member.setUpdatedAt(LocalDateTime.now());

        ChannelMembers unmutedMember = channelMembersRepo.save(member);

        log.info("Member {} unmuted successfully", targetUserId);

        // 멤버 음소거 해제 이벤트 전송
        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId + "/member-unmuted",
                ChannelEvent.builder()
                        .eventType("MEMBER_UNMUTED")
                        .channelId(channelId)
                        .userId(unmutedByUserId)
                        .data(Map.of(
                                "unmutedUserId", targetUserId,
                                "unmutedByUserId", unmutedByUserId
                        ))
                        .build()
        );

        return toChannelMemberResponse(unmutedMember);
    }

    @Override
    public ChannelMemberResponse banMember(String channelId, String targetUserId, String bannedByUserId) {
        log.info("Banning member {} from channel {} by {}", targetUserId, channelId, bannedByUserId);

        return updateMemberStatus(channelId, targetUserId, MemberStatus.BANNED, bannedByUserId);
    }

    @Override
    public ChannelMemberResponse unbanMember(String channelId, String targetUserId, String unbannedByUserId) {
        log.info("Unbanning member {} from channel {} by {}", targetUserId, channelId, unbannedByUserId);

        return updateMemberStatus(channelId, targetUserId, MemberStatus.ACTIVE, unbannedByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(String channelId, String userId, String permission) {
        // 권한 확인 로직 - null 처리 추가
        log.debug("Checking permission for channelId: {}, userId: {}, permission: {}",
                channelId, userId, permission);

        Boolean result = channelAuthoritiesRepo.hasPermission(channelId, userId, permission);
        boolean hasPermission = result != null && result;

        log.debug("Permission check result: {}", hasPermission);
        return hasPermission;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getOnlineMembers(String channelId, String userId) {
        validateChannelMembership(channelId, userId);

        // 본초 자오선 기준 최근 접속 확인
        LocalDateTime onlineThreshold = LocalDateTime.now().minusMinutes(5);

        List<ChannelMembers> onlineMembers = channelMembersRepo
                .findByChannelIdAndStatusAndLastSeenAtAfter(
                        channelId,
                        MemberStatus.ACTIVE,
                        onlineThreshold
                );

        return onlineMembers.stream()
                .map(this::toChannelMemberResponse)
                .toList();
    }

    // ===== Private Helper Methods =====

    private void validateChannelMembership(String channelId, String userId) {
        if (!channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new CustomException(ErrorCode.NOT_CHANNEL_MEMBER);
        }
    }

    private boolean hasAddMemberPermission(String channelId, String userId) {
        return hasPermission(channelId, userId, "ADD_MEMBER");
    }

    private boolean hasRemoveMemberPermission(String channelId, String userId) {
        return hasPermission(channelId, userId, "REMOVE_MEMBER");
    }

    private boolean hasUpdateMemberPermission(String channelId, String userId) {
        return hasPermission(channelId, userId, "UPDATE_MEMBER");
    }

    private boolean hasUpdateRolePermission(String channelId, String userId) {
        return hasPermission(channelId, userId, "UPDATE_ROLE");
    }

    private boolean hasMutePermission(String channelId, String userId) {
        return hasPermission(channelId, userId, "MUTE_MEMBER");
    }

    private ChannelMemberResponse toChannelMemberResponse(ChannelMembers member) {
        ChannelAuthorities authorities = channelAuthoritiesRepo.findByChannelIdAndUserId(
                member.getChannelId(), member.getUserId()
        );
        String roleId = null;

        if (authorities != null) {
            roleId = authorities.getRoleId();
        }

        boolean isOnline = member.getLastSeenAt() != null &&
                member.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(1));


        return ChannelMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .lastSeenAt(member.getLastSeenAt())
                .lastReadMessageId(member.getLastReadMessageId())
                .notificationLevel(member.getNotificationLevel())
                .isMuted(member.getIsMuted())
                .mutedUntil(member.getMutedUntil())
                .isOnline(isOnline)
                .roleId(roleId)
                // 추가 정보는 별도 조회 후 설정
                .build();
    }

    // ChannelServiceImpl.java에 추가할 메서드들

    private void validateChannelJoin(Channels channel, ChannelJoinRequest request, String userId) {
        // 아카이브된 채널 체크
        if (channel.getIsArchived() != null && channel.getIsArchived()) {
            throw new CustomException(ErrorCode.ARCHIVED_CHANNEL);
        }

        // 최대 멤버 수 체크
        if (channel.getMaxMembers() != null &&
                channel.getMemberCount() != null &&
                channel.getMemberCount() >= channel.getMaxMembers()) {
            throw new CustomException(ErrorCode.CHANNEL_FULL);
        }

        // 비공개 채널 비밀번호 체크
        if (channel.getChannelType() == ChannelType.PRIVATE &&
                channel.getPassword() != null) {
            if (request.getPassword() == null ||
                    !request.getPassword().equals(channel.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_CHANNEL_PASSWORD);
            }
        }

        // 승인 필요한 채널 체크
        if (channel.getRequiresApproval() != null && channel.getRequiresApproval()) {
            // 승인 요청 로직 - 나중에 구현
            throw new CustomException(ErrorCode.CHANNEL_APPROVAL_REQUIRED);
        }
    }

    private void enrichChannelResponse(ChannelResponse response, String userId) {
        // 사용자별 추가 정보 설정
        Optional<ChannelMembers> memberOpt = channelMembersRepo.findByChannelIdAndUserId(
                response.getId(), userId);

        if (memberOpt.isPresent()) {
            ChannelMembers member = memberOpt.get();
            response.setIsMember(true);
            response.setIsMuted(member.getIsMuted());
            // 안 읽은 메시지 수는 나중에 구현
            response.setUnreadMessageCount(0);
        } else {
            response.setIsMember(false);
            response.setIsMuted(false);
            response.setUnreadMessageCount(0);
        }
    }
}