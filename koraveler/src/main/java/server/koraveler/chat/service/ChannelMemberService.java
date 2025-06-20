// 3. ChannelMemberService.java
package server.koraveler.chat.service;

import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.model.enums.NotificationLevel;

import java.util.List;

public interface ChannelMemberService {

    // 멤버 추가
    ChannelMemberResponse addMember(String channelId, String userId, String addedByUserId);

    // 멤버 제거
    void removeMember(String channelId, String userId, String removedByUserId);

    // 멤버 목록 조회
    List<ChannelMemberResponse> getChannelMembers(String channelId, String userId);

    // 멤버 상태 변경
    ChannelMemberResponse updateMemberStatus(String channelId, String targetUserId,
                                             MemberStatus status, String updatedByUserId);

    // 멤버 권한 변경
    ChannelMemberResponse updateMemberRole(String channelId, String targetUserId,
                                           String roleId, String updatedByUserId);

    // 멤버 닉네임 변경
    ChannelMemberResponse updateMemberNickname(String channelId, String nickname, String userId);

    // 알림 설정 변경
    ChannelMemberResponse updateNotificationSettings(String channelId, NotificationLevel level,
                                                     Boolean enabled, String userId);

    // 멤버 음소거/음소거 해제
    ChannelMemberResponse muteMember(String channelId, String targetUserId,
                                     Long muteMinutes, String mutedByUserId);
    ChannelMemberResponse unmuteMember(String channelId, String targetUserId, String unmutedByUserId);

    // 멤버 밴/밴 해제
    ChannelMemberResponse banMember(String channelId, String targetUserId, String bannedByUserId);
    ChannelMemberResponse unbanMember(String channelId, String targetUserId, String unbannedByUserId);

    // 멤버 권한 확인
    boolean hasPermission(String channelId, String userId, String permission);

    // 온라인 멤버 조회
    List<ChannelMemberResponse> getOnlineMembers(String channelId, String userId);
}
