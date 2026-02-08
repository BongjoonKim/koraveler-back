// 2. ChannelService.java
package server.koraveler.chat.service;

import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;

import java.util.List;

public interface ChannelService {

    // 채널 생성
    ChannelResponse createChannel(ChannelCreateRequest request, String userId);

    // 채널 수정
    ChannelResponse updateChannel(String channelId, ChannelUpdateRequest request, String userId);

    // 채널 삭제 (소프트 삭제/아카이브)
    void deleteChannel(String channelId, String userId);
    void archiveChannel(String channelId, String userId);
    void unarchiveChannel(String channelId, String userId);

    // 채널 조회
    ChannelResponse getChannel(String channelId, String userId);

    // 사용자 참여 채널 목록
    ChannelListResponse getUserChannels(String userId, PageRequest pageRequest);

    // 공개 채널 목록
    ChannelListResponse getPublicChannels(PageRequest pageRequest);

    // 채널 검색
    ChannelListResponse searchChannels(String keyword, String userId, PageRequest pageRequest);

    // 채널 참여
    ChannelResponse joinChannel(String channelId, ChannelJoinRequest request, String userId);

    // 채널 탈퇴
    void leaveChannel(String channelId, String userId);

    // 채널 설정 변경
    ChannelResponse updateChannelSettings(String channelId, ChannelSettingsRequest request, String userId);

    // 채널 초대 링크 생성
    String generateInviteLink(String channelId, String userId);

    // 초대 링크로 참여
    ChannelResponse joinByInviteLink(String inviteCode, String userId);
}