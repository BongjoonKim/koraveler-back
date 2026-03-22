package server.koraveler.travel.service;

import server.koraveler.travel.model.dto.TravelChannelCreateRequest;
import server.koraveler.travel.model.dto.TravelChannelResponse;
import server.koraveler.travel.model.dto.TravelChannelUpdateRequest;

import java.util.List;

public interface TravelChannelService {

    // 채널 생성 (Chat 채널 생성 + 브릿지 레코드 저장 + 멤버 동기화)
    TravelChannelResponse createChannel(String travelId, TravelChannelCreateRequest request, String userId);

    // 채널 목록 조회
    List<TravelChannelResponse> getChannels(String travelId, String userId);

    // 단일 채널 조회
    TravelChannelResponse getChannel(String travelId, String travelChannelId, String userId);

    // 브릿지 메타데이터 수정
    TravelChannelResponse updateChannel(String travelId, String travelChannelId, TravelChannelUpdateRequest request, String userId);

    // 채널 삭제 (soft delete + 채널 아카이브)
    void deleteChannel(String travelId, String travelChannelId, String userId);

    // Travel 멤버 → 채널 멤버 동기화
    void syncMembers(String travelId, String travelChannelId, String userId);
}
