// 3. ChannelMemberServiceImpl.java
package server.koraveler.chat.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.koraveler.chat.dto.response.ChannelMemberResponse;
import server.koraveler.chat.model.entities.ChannelMembers;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.model.enums.NotificationLevel;
import server.koraveler.chat.repository.ChannelMembersRepo;
import server.koraveler.chat.repository.ChannelAuthoritiesRepo;
import server.koraveler.chat.service.ChannelMemberService;
import server.koraveler.chat.exception.CustomException;
import server.koraveler.chat.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChannelMemberServiceImpl implements ChannelMemberService {

    private final ChannelMembersRepo channelMembersRepo;
    private final ChannelAuthoritiesRepo channelAuthoritiesRepo;

    @Override
    public ChannelMemberResponse addMember(String channelId, String userId, String addedByUserId) {
        log.info("Adding member {} to channel {} by {}", userId, channelId, addedByUserId);

        // 이미 멤버인지 확인
        if (channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new CustomException(ErrorCode.ALREADY_CHANNEL_MEMBER);
        }

        // 멤버 추가 권한 확인
        if (!addedByUserId.equals(userId) && !hasAddMemberPermission(channelId, addedByUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ADD_MEMBER);
        }

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

        ChannelMembers savedMember = channelMembersRepo.save(member);

        log.info("Member {} added to channel {} successfully", userId, channelId);
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
        // 권한 확인 로직
        // ChannelAuthorities와 ChannelRoles를 조인해서 권한 확인
        return channelAuthoritiesRepo.hasPermission(channelId, userId, permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelMemberResponse> getOnlineMembers(String channelId, String userId) {
        // 채널 접근 권한 확인
        validateChannelMembership(channelId, userId);

        List<ChannelMembers> onlineMembers = channelMembersRepo.findOnlineMembers(channelId);

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
                // 추가 정보는 별도 조회 후 설정
                .build();
    }
}