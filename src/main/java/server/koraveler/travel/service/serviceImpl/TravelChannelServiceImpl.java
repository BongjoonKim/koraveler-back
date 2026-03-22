package server.koraveler.travel.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.koraveler.chat.dto.request.ChannelCreateRequest;
import server.koraveler.chat.dto.response.ChannelResponse;
import server.koraveler.chat.repository.ChannelsRepo;
import server.koraveler.chat.model.entities.Channels;
import server.koraveler.chat.service.ChannelMemberService;
import server.koraveler.chat.service.ChannelService;
import server.koraveler.error.CustomException;
import server.koraveler.error.ErrorCode;
import server.koraveler.travel.model.dto.TravelChannelCreateRequest;
import server.koraveler.travel.model.dto.TravelChannelResponse;
import server.koraveler.travel.model.dto.TravelChannelUpdateRequest;
import server.koraveler.travel.model.entities.TravelChannel;
import server.koraveler.travel.model.entities.TravelUsers;
import server.koraveler.travel.model.entities.Travels;
import server.koraveler.travel.model.enums.TravelRole;
import server.koraveler.travel.model.mapper.TravelChannelMapper;
import server.koraveler.travel.repo.TravelChannelRepo;
import server.koraveler.travel.repo.TravelUsersRepo;
import server.koraveler.travel.repo.TravelsRepo;
import server.koraveler.travel.service.TravelChannelService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelChannelServiceImpl implements TravelChannelService {

    private final TravelChannelRepo travelChannelRepo;
    private final TravelsRepo travelsRepo;
    private final TravelUsersRepo travelUsersRepo;
    private final TravelChannelMapper travelChannelMapper;
    private final ChannelService channelService;
    private final ChannelMemberService channelMemberService;
    private final ChannelsRepo channelsRepo;

    // ==================== 채널 생성 ====================

    @Override
    public TravelChannelResponse createChannel(String travelId, TravelChannelCreateRequest request, String userId) {
        log.info("Creating travel channel for travel: {} by user: {}", travelId, userId);

        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        // Travel 멤버 목록으로 초기 멤버 구성
        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        List<String> memberUserIds = members.stream()
                .map(TravelUsers::getUserId)
                .filter(id -> !id.equals(userId)) // 생성자는 ChannelService에서 자동 추가
                .toList();

        // 1. Chat 모듈의 ChannelService로 실제 채널 생성
        ChannelCreateRequest channelCreateRequest = travelChannelMapper.toChannelCreateRequest(request, memberUserIds);
        ChannelResponse channelResponse = channelService.createChannel(channelCreateRequest, userId);

        // 2. 중복 체크
        if (travelChannelRepo.existsByTravelIdAndChannelIdAndDeletedFalse(travelId, channelResponse.getId())) {
            throw new CustomException(ErrorCode.CHANNEL_ALREADY_LINKED);
        }

        // 3. TravelChannel 브릿지 레코드 저장
        TravelChannel travelChannel = travelChannelMapper.toEntity(request, travelId, channelResponse.getId(), userId);
        TravelChannel savedChannel = travelChannelRepo.save(travelChannel);

        // 4. Travels.channelIds 동기화 (하위 호환)
        if (travel.getChannelIds() == null) {
            travel.setChannelIds(new ArrayList<>());
        }
        travel.getChannelIds().add(channelResponse.getId());
        travel.setUpdated(LocalDateTime.now());
        travel.setUpdatedUser(userId);
        travelsRepo.save(travel);

        log.info("Travel channel created: {} -> channel: {}", savedChannel.getId(), channelResponse.getId());
        return travelChannelMapper.toResponse(savedChannel, channelResponse);
    }

    // ==================== 채널 목록 조회 ====================

    @Override
    public List<TravelChannelResponse> getChannels(String travelId, String userId) {
        findTravelById(travelId);
        validateMembership(travelId, userId);

        List<TravelChannel> travelChannels = travelChannelRepo.findByTravelIdAndDeletedFalseOrderByDisplayOrderAsc(travelId);
        if (travelChannels.isEmpty()) {
            return new ArrayList<>();
        }

        // N+1 방지: channelId 리스트로 일괄 조회
        List<String> channelIds = travelChannels.stream()
                .map(TravelChannel::getChannelId)
                .toList();
        List<Channels> channels = channelsRepo.findByIdIn(channelIds);
        Map<String, Channels> channelMap = channels.stream()
                .collect(Collectors.toMap(Channels::getId, c -> c));

        return travelChannels.stream()
                .map(tc -> {
                    Channels channel = channelMap.get(tc.getChannelId());
                    ChannelResponse channelResponse = channel != null
                            ? buildChannelResponse(channel)
                            : null;
                    return travelChannelMapper.toResponse(tc, channelResponse);
                })
                .toList();
    }

    // ==================== 단일 채널 조회 ====================

    @Override
    public TravelChannelResponse getChannel(String travelId, String travelChannelId, String userId) {
        findTravelById(travelId);
        validateMembership(travelId, userId);

        TravelChannel travelChannel = findTravelChannelById(travelChannelId);
        ChannelResponse channelResponse = getChannelResponseSafe(travelChannel.getChannelId(), userId);
        return travelChannelMapper.toResponse(travelChannel, channelResponse);
    }

    // ==================== 채널 메타데이터 수정 ====================

    @Override
    public TravelChannelResponse updateChannel(String travelId, String travelChannelId, TravelChannelUpdateRequest request, String userId) {
        log.info("Updating travel channel: {} by user: {}", travelChannelId, userId);

        findTravelById(travelId);
        validateAdminRole(travelId, userId);

        TravelChannel travelChannel = findTravelChannelById(travelChannelId);

        if (request.getContextType() != null) travelChannel.setContextType(request.getContextType());
        if (request.getContextId() != null) travelChannel.setContextId(request.getContextId());
        if (request.getChannelPurpose() != null) travelChannel.setChannelPurpose(request.getChannelPurpose());
        if (request.getIsPinned() != null) travelChannel.setIsPinned(request.getIsPinned());
        if (request.getDisplayOrder() != null) travelChannel.setDisplayOrder(request.getDisplayOrder());

        travelChannel.setUpdated(LocalDateTime.now());
        travelChannel.setUpdatedUser(userId);
        TravelChannel updated = travelChannelRepo.save(travelChannel);

        ChannelResponse channelResponse = getChannelResponseSafe(travelChannel.getChannelId(), userId);
        return travelChannelMapper.toResponse(updated, channelResponse);
    }

    // ==================== 채널 삭제 ====================

    @Override
    public void deleteChannel(String travelId, String travelChannelId, String userId) {
        log.info("Deleting travel channel: {} by user: {}", travelChannelId, userId);

        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        TravelChannel travelChannel = findTravelChannelById(travelChannelId);

        // 1. 브릿지 soft delete
        travelChannel.setIsDeleted(true);
        travelChannel.setUpdated(LocalDateTime.now());
        travelChannel.setUpdatedUser(userId);
        travelChannelRepo.save(travelChannel);

        // 2. Travels.channelIds에서 제거 (하위 호환)
        if (travel.getChannelIds() != null) {
            travel.getChannelIds().remove(travelChannel.getChannelId());
            travel.setUpdated(LocalDateTime.now());
            travel.setUpdatedUser(userId);
            travelsRepo.save(travel);
        }

        // 3. Chat 채널 아카이브 (실패해도 브릿지 삭제는 유지)
        try {
            channelService.archiveChannel(travelChannel.getChannelId(), userId);
        } catch (Exception e) {
            log.warn("Failed to archive chat channel: {}. Bridge record deleted.", travelChannel.getChannelId(), e);
        }

        log.info("Travel channel deleted: {}", travelChannelId);
    }

    // ==================== 멤버 동기화 ====================

    @Override
    public void syncMembers(String travelId, String travelChannelId, String userId) {
        log.info("Syncing members for travel channel: {} by user: {}", travelChannelId, userId);

        findTravelById(travelId);
        validateAdminRole(travelId, userId);

        TravelChannel travelChannel = findTravelChannelById(travelChannelId);
        String channelId = travelChannel.getChannelId();

        List<TravelUsers> travelMembers = travelUsersRepo.findByTravelId(travelId);

        for (TravelUsers member : travelMembers) {
            try {
                channelMemberService.addMember(channelId, member.getUserId(), userId);
            } catch (CustomException e) {
                // 이미 멤버인 경우 무시
                if (ErrorCode.ALREADY_CHANNEL_MEMBER.getCode().equals(e.getCode())) {
                    continue;
                }
                throw e;
            }
        }

        log.info("Member sync completed for travel channel: {}", travelChannelId);
    }

    // ==================== Helper Methods ====================

    private Travels findTravelById(String travelId) {
        return travelsRepo.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));
    }

    private TravelChannel findTravelChannelById(String travelChannelId) {
        return travelChannelRepo.findById(travelChannelId)
                .filter(tc -> !Boolean.TRUE.equals(tc.getIsDeleted()))
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_CHANNEL_NOT_FOUND));
    }

    private void validateMembership(String travelId, String userId) {
        if (!travelUsersRepo.existsByTravelIdAndUserId(travelId, userId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_MEMBER);
        }
    }

    private void validateAdminRole(String travelId, String userId) {
        TravelUsers member = travelUsersRepo.findByTravelIdAndUserId(travelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TRAVEL_MEMBER));

        if (member.getRole() != TravelRole.ADMIN) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MEMBER_MANAGE);
        }
    }

    // ChannelResponse를 안전하게 조회 (채널 삭제 시에도 NPE 방지)
    private ChannelResponse getChannelResponseSafe(String channelId, String userId) {
        try {
            return channelService.getChannel(channelId, userId);
        } catch (Exception e) {
            log.warn("Failed to fetch channel info: {}", channelId);
            return null;
        }
    }

    // Channels 엔티티 → ChannelResponse 간단 변환 (N+1 방지용)
    private ChannelResponse buildChannelResponse(Channels channel) {
        return ChannelResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .channelType(channel.getChannelType())
                .memberCount(channel.getMemberCount())
                .lastMessageAt(channel.getLastMessageAt())
                .isArchived(channel.getIsArchived())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }
}
