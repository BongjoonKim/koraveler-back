// 2. ChannelServiceImpl.java
package server.koraveler.chat.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import server.koraveler.chat.dto.mapper.ChannelMapper;
import server.koraveler.chat.model.entities.ChannelAuthorities;
import server.koraveler.chat.model.entities.Channels;
import server.koraveler.chat.model.entities.ChannelMembers;
import server.koraveler.chat.model.enums.ChannelType;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.model.enums.NotificationLevel;
import server.koraveler.chat.repository.ChannelAuthoritiesRepo;
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
    private final ChannelAuthoritiesRepo channelAuthoritiesRepo;  // 추가
    private final ChannelMemberService channelMemberService;
    private final ChannelMapper channelMapper;

    // ChannelServiceImpl.java에 추가할 메서드들

    // ===== 디버깅 메서드들 (개발 환경에서만 사용) =====

    /**
     * 채널 조회 문제 디버깅을 위한 메서드
     */
    public Map<String, Object> debugChannelQuery(String userId) {
        Map<String, Object> result = new HashMap<>();

        log.info("=== DEBUG: Channel Query for User {} ===", userId);

        // Step 1: ChannelMembers 확인
        List<ChannelMembers> memberships = channelMembersRepo.findByUserIdAndStatus(userId, MemberStatus.ACTIVE);
        List<String> memberChannelIds = memberships.stream()
                .map(ChannelMembers::getChannelId)
                .collect(Collectors.toList());

        result.put("membershipCount", memberships.size());
        result.put("memberChannelIds", memberChannelIds);
        log.info("Found {} memberships with channel IDs: {}", memberships.size(), memberChannelIds);

        // Step 2: 생성자 채널 확인
        List<Channels> createdChannels = channelsRepo.findByCreatedUserId(userId);
        List<String> createdChannelIds = createdChannels.stream()
                .map(Channels::getId)
                .collect(Collectors.toList());

        result.put("createdChannelsCount", createdChannels.size());
        result.put("createdChannelIds", createdChannelIds);
        result.put("createdChannels", createdChannels.stream()
                .map(ch -> Map.of(
                        "id", ch.getId(),
                        "name", ch.getName(),
                        "isArchived", ch.getIsArchived() != null ? ch.getIsArchived() : false
                ))
                .collect(Collectors.toList()));
        log.info("Found {} channels created by user", createdChannels.size());

        // Step 3: 모든 채널 ID 합치기
        Set<String> allChannelIds = new HashSet<>();
        allChannelIds.addAll(memberChannelIds);
        allChannelIds.addAll(createdChannelIds);
        List<String> finalChannelIds = new ArrayList<>(allChannelIds);

        result.put("finalChannelIds", finalChannelIds);
        result.put("totalUniqueChannels", finalChannelIds.size());
        log.info("Total unique channel IDs: {}", finalChannelIds.size());

        // Step 4: 각 방법으로 채널 조회 테스트
        if (!finalChannelIds.isEmpty()) {
            // 4-1. findByIdIn 테스트
            List<Channels> byIdIn = channelsRepo.findByIdIn(finalChannelIds);
            result.put("findByIdIn_count", byIdIn.size());
            result.put("findByIdIn_results", byIdIn.stream()
                    .map(ch -> Map.of("id", ch.getId(), "name", ch.getName()))
                    .collect(Collectors.toList()));
            log.info("findByIdIn returned {} channels", byIdIn.size());

            // 4-2. findAllById 테스트
            List<Channels> byAllById = channelsRepo.findAllById(finalChannelIds);
            result.put("findAllById_count", byAllById.size());
            log.info("findAllById returned {} channels", byAllById.size());

            // 4-3. 페이징 포함 테스트
            Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
            List<Channels> byIdInWithPageable = channelsRepo.findByIdIn(finalChannelIds, pageable);
            result.put("findByIdIn_pageable_count", byIdInWithPageable.size());
            log.info("findByIdIn with pageable returned {} channels", byIdInWithPageable.size());
        }

        // Step 5: 특정 채널 ID로 직접 테스트 (하드코딩)
        String testChannelId = "68c3e0189c2ca512d42168df";
        Optional<Channels> testChannel = channelsRepo.findById(testChannelId);
        result.put("hardcodedChannelTest", Map.of(
                "channelId", testChannelId,
                "found", testChannel.isPresent(),
                "details", testChannel.map(ch -> Map.of(
                        "name", ch.getName(),
                        "createdUserId", ch.getCreatedUserId(),
                        "isArchived", ch.getIsArchived() != null ? ch.getIsArchived() : false
                )).orElse(null)
        ));

        log.info("=== DEBUG COMPLETE ===");
        return result;
    }

    /**
     * 특정 채널의 멤버십 상태 확인
     */
    public Map<String, Object> debugChannelMembership(String channelId, String userId) {
        Map<String, Object> result = new HashMap<>();

        // 채널 정보
        Optional<Channels> channelOpt = channelsRepo.findById(channelId);
        result.put("channelExists", channelOpt.isPresent());

        if (channelOpt.isPresent()) {
            Channels channel = channelOpt.get();
            result.put("channel", Map.of(
                    "id", channel.getId(),
                    "name", channel.getName(),
                    "createdUserId", channel.getCreatedUserId(),
                    "memberCount", channel.getMemberCount(),
                    "isArchived", channel.getIsArchived() != null ? channel.getIsArchived() : false
            ));

            // 생성자인지 확인
            result.put("isCreator", channel.getCreatedUserId().equals(userId));
        }

        // 멤버십 정보
        Optional<ChannelMembers> membershipOpt = channelMembersRepo.findByChannelIdAndUserId(channelId, userId);
        result.put("isMember", membershipOpt.isPresent());

        if (membershipOpt.isPresent()) {
            ChannelMembers membership = membershipOpt.get();
            result.put("membership", Map.of(
                    "status", membership.getStatus(),
//                    "role", membership.getRole(),
                    "joinedAt", membership.getJoinedAt().toString()
            ));
        }

        // 모든 멤버 리스트
        List<ChannelMembers> allMembers = channelMembersRepo.findByChannelId(channelId);
        result.put("totalMembers", allMembers.size());
        result.put("activeMembers", allMembers.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .count());

        return result;
    }

    /**
     * 채널 생성과 동시에 멤버십 확인 (문제 재현용)
     */
    public Map<String, Object> createChannelWithDebug(ChannelCreateRequest request, String userId) {
        Map<String, Object> result = new HashMap<>();

        log.info("=== DEBUG: Creating channel with monitoring ===");

        // Step 1: 채널 생성 전 상태
        List<Channels> beforeChannels = channelsRepo.findByCreatedUserId(userId);
        result.put("channelsBeforeCreate", beforeChannels.size());

        // Step 2: 채널 생성
        ChannelResponse createdChannel = createChannel(request, userId);
        result.put("createdChannel", Map.of(
                "id", createdChannel.getId(),
                "name", createdChannel.getName()
        ));
        log.info("Channel created with ID: {}", createdChannel.getId());

        // Step 3: 채널 생성 후 즉시 확인
        Optional<Channels> savedChannel = channelsRepo.findById(createdChannel.getId());
        result.put("channelSavedInDB", savedChannel.isPresent());

        // Step 4: 멤버십 확인
        Optional<ChannelMembers> membership = channelMembersRepo.findByChannelIdAndUserId(
                createdChannel.getId(), userId);
        result.put("membershipCreated", membership.isPresent());
        if (membership.isPresent()) {
            result.put("membershipStatus", membership.get().getStatus().toString());
        }

        // Step 5: 다시 조회 테스트
        List<String> channelIds = List.of(createdChannel.getId());

        // findById
        Optional<Channels> byId = channelsRepo.findById(createdChannel.getId());
        result.put("findById_works", byId.isPresent());

        // findByIdIn
        List<Channels> byIdIn = channelsRepo.findByIdIn(channelIds);
        result.put("findByIdIn_works", !byIdIn.isEmpty());
        result.put("findByIdIn_count", byIdIn.size());

        // findAllById
        List<Channels> byAllById = channelsRepo.findAllById(channelIds);
        result.put("findAllById_works", !byAllById.isEmpty());
        result.put("findAllById_count", byAllById.size());

        log.info("=== DEBUG COMPLETE: All queries tested ===");
        return result;
    }

    /**
     * 기존 채널에 멤버십 수동 추가 (복구용)
     */
    public String fixMissingMembership(String channelId, String userId) {
        Optional<Channels> channelOpt = channelsRepo.findById(channelId);
        if (channelOpt.isEmpty()) {
            return "Channel not found: " + channelId;
        }

        Channels channel = channelOpt.get();

        // 이미 멤버인지 확인
        if (channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            Optional<ChannelMembers> existing = channelMembersRepo.findByChannelIdAndUserId(channelId, userId);
            if (existing.isPresent() && existing.get().getStatus() != MemberStatus.ACTIVE) {
                // 상태만 ACTIVE로 변경
                ChannelMembers member = existing.get();
                member.setStatus(MemberStatus.ACTIVE);
                channelMembersRepo.save(member);
                return "Updated existing membership to ACTIVE";
            }
            return "User is already an active member";
        }

        // 멤버십 추가
        channelMemberService.addMember(channelId, userId, userId);

        // *** 추가 필요: 권한도 추가 ***
        if (!channelAuthoritiesRepo.existsByChannelIdAndUserId(channelId, userId)) {
            // 채널 생성자인지 확인
            boolean isOwner = channel.getCreatedUserId().equals(userId);

            ChannelAuthorities authority = ChannelAuthorities.builder()
                    .channelId(channelId)
                    .userId(userId)
                    .roleId(isOwner ? "OWNER" : "MEMBER")
                    .permissions(isOwner ?
                            List.of("ADD_MEMBER", "REMOVE_MEMBER", "UPDATE_MEMBER",
                                    "UPDATE_ROLE", "MUTE_MEMBER", "BAN_MEMBER",
                                    "DELETE_MESSAGE", "UPDATE_CHANNEL", "DELETE_CHANNEL") :
                            List.of("READ_MESSAGE", "SEND_MESSAGE", "LEAVE_CHANNEL"))
                    .grantedByUserId(userId)
                    .grantedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            channelAuthoritiesRepo.save(authority);
        }

        // 멤버 수 업데이트
        if (channel.getMemberCount() == null) {
            channel.setMemberCount(1);
        } else {
            channel.setMemberCount(channel.getMemberCount() + 1);
        }
        channelsRepo.save(channel);

        return "Membership added successfully for user: " + userId;
    }

    @Override
    public ChannelResponse createChannel(ChannelCreateRequest request, String userId) {
        log.info("Creating channel: {} by user: {}", request.getName(), userId);

        // 채널 이름 중복 확인
        if (channelsRepo.existsByNameAndChannelType(request.getName(), request.getChannelType())) {
            throw new CustomException(ErrorCode.DUPLICATE_CHANNEL_NAME);
        }

        // 채널 생성
        Channels channel = channelMapper.toEntity(request, userId);
        channel.setMemberCount(1); // 초기값 설정
        Channels savedChannel = channelsRepo.save(channel);

        // 생성자를 채널 멤버로 추가
        ChannelMembers creator = ChannelMembers.builder()
                .channelId(savedChannel.getId())
                .userId(userId)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .notificationLevel(NotificationLevel.ALL)
                .isMuted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        channelMembersRepo.save(creator);
        log.info("Channel creator membership created for user: {} in channel: {}", userId, savedChannel.getId());

        // 생성자에게 채널 관리자 권한 부여
        ChannelAuthorities ownerAuthority = ChannelAuthorities.builder()
                .channelId(savedChannel.getId())
                .userId(userId)
                .roleId("OWNER")
                .permissions(List.of(
                        "ADD_MEMBER", "REMOVE_MEMBER", "UPDATE_MEMBER",
                        "UPDATE_ROLE", "MUTE_MEMBER", "BAN_MEMBER",
                        "DELETE_MESSAGE", "UPDATE_CHANNEL", "DELETE_CHANNEL"
                ))
                .grantedByUserId(userId)
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        channelAuthoritiesRepo.save(ownerAuthority);
        log.info("Channel owner authorities created for user: {} in channel: {}", userId, savedChannel.getId());

        // 초기 멤버들 추가 (있는 경우)
        if (request.getInitialMemberIds() != null && !request.getInitialMemberIds().isEmpty()) {
            int additionalMembers = 0;
            for (String memberId : request.getInitialMemberIds()) {
                if (!memberId.equals(userId)) {
                    try {
                        // 멤버 추가
                        ChannelMembers member = ChannelMembers.builder()
                                .channelId(savedChannel.getId())
                                .userId(memberId)
                                .status(MemberStatus.ACTIVE)
                                .joinedAt(LocalDateTime.now())
                                .lastSeenAt(LocalDateTime.now())
                                .notificationLevel(NotificationLevel.ALL)
                                .isMuted(false)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        channelMembersRepo.save(member);
                        additionalMembers++;

                        // 일반 멤버 권한 부여
                        ChannelAuthorities memberAuthority = ChannelAuthorities.builder()
                                .channelId(savedChannel.getId())
                                .userId(memberId)
                                .roleId("MEMBER")
                                .permissions(List.of("READ_MESSAGE", "SEND_MESSAGE", "LEAVE_CHANNEL"))
                                .grantedByUserId(userId)
                                .grantedAt(LocalDateTime.now())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        channelAuthoritiesRepo.save(memberAuthority);
                    } catch (Exception e) {
                        log.error("Failed to add initial member {} to channel {}: {}",
                                memberId, savedChannel.getId(), e.getMessage());
                    }
                }
            }

            // 실제 추가된 멤버 수로 업데이트
            savedChannel.setMemberCount(1 + additionalMembers);
            savedChannel = channelsRepo.save(savedChannel);
        }

        log.info("Channel created successfully: {} with {} members",
                savedChannel.getId(), savedChannel.getMemberCount());

        ChannelResponse response = channelMapper.toResponse(savedChannel);
        enrichChannelResponse(response, userId);
        return response;
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
        // 1. 멤버로 참여한 채널 ID 조회
        List<String> memberChannelIds = channelMembersRepo.findByUserIdAndStatus(userId, MemberStatus.ACTIVE)
                .stream()
                .map(ChannelMembers::getChannelId)
                .collect(Collectors.toList());

        // 2. 생성자로서의 채널 ID 조회
        List<String> createdChannelIds = channelsRepo.findByCreatedUserIdAndIsArchivedFalse(userId)
                .stream()
                .map(Channels::getId)
                .collect(Collectors.toList());

        // 3. 중복 제거하여 합치기
        Set<String> allChannelIds = new HashSet<>();
        allChannelIds.addAll(memberChannelIds);
        allChannelIds.addAll(createdChannelIds);

        if (allChannelIds.isEmpty()) {
            return ChannelListResponse.builder()
                    .channels(List.of())
                    .totalCount(0)
                    .hasNext(false)
                    .build();
        }

        Pageable pageable = createPageable(pageRequest);

        // 4. 채널 조회
        List<Channels> channels = channelsRepo.findByIdIn(new ArrayList<>(allChannelIds), pageable);

        // 5. 응답 생성 시 실제 멤버 수 반영
        List<ChannelResponse> channelResponses = channels.stream()
                .filter(channel -> channel.getIsArchived() == null || !channel.getIsArchived())
                .map(channel -> {
                    ChannelResponse response = channelMapper.toResponse(channel);
                    enrichChannelResponse(response, userId);
                    return response;
                })
                .toList();

        return ChannelListResponse.builder()
                .channels(channelResponses)
                .totalCount(allChannelIds.size())
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
        ChannelMembers newMember = ChannelMembers.builder()
                .channelId(channelId)
                .userId(userId)
                .status(MemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .notificationLevel(NotificationLevel.ALL)
                .isMuted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        channelMembersRepo.save(newMember);

        // 일반 멤버 권한 부여
        ChannelAuthorities memberAuthority = ChannelAuthorities.builder()
                .channelId(channelId)
                .userId(userId)
                .roleId("MEMBER")
                .permissions(List.of("READ_MESSAGE", "SEND_MESSAGE", "LEAVE_CHANNEL"))
                .grantedByUserId(userId)
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        channelAuthoritiesRepo.save(memberAuthority);

        // 실제 활성 멤버 수로 업데이트
        Long actualMemberCount = channelMembersRepo.countActiveMembers(channelId);
        channel.setMemberCount(actualMemberCount.intValue());
        channelsRepo.save(channel);

        log.info("User {} joined channel {} successfully. Total members: {}",
                userId, channelId, actualMemberCount);

        ChannelResponse response = channelMapper.toResponse(channel);
        enrichChannelResponse(response, userId);
        return response;
    }


    // 5. 채널 탈퇴 시 멤버 수 동기화
    @Override
    public void leaveChannel(String channelId, String userId) {
        log.info("User {} leaving channel: {}", userId, channelId);

        Channels channel = channelsRepo.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // 채널 멤버인지 확인
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        // 멤버 상태를 LEFT로 변경 (소프트 삭제)
        member.setStatus(MemberStatus.LEFT);
        member.setLeftAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        channelMembersRepo.save(member);

        // 권한 정보 삭제
        ChannelAuthorities authority = channelAuthoritiesRepo.findByChannelIdAndUserId(channelId, userId);
        if (authority != null) {
            channelAuthoritiesRepo.delete(authority);
        }

        // 실제 활성 멤버 수로 업데이트
        Long actualMemberCount = channelMembersRepo.countActiveMembers(channelId);
        channel.setMemberCount(actualMemberCount.intValue());
        channelsRepo.save(channel);

        log.info("User {} left channel {} successfully. Remaining members: {}",
                userId, channelId, actualMemberCount);
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
        // 실제 활성 멤버 수를 DB에서 조회
        Long actualMemberCount = channelMembersRepo.countActiveMembers(response.getId());
        response.setMemberCount(actualMemberCount.intValue());

        // 사용자별 추가 정보 설정
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(
                response.getId(), userId).orElse(null);

        if (member != null) {
            response.setIsMember(true);
            response.setIsMuted(member.getIsMuted());
            response.setUnreadMessageCount(calculateUnreadCount(response.getId(), userId));
            // 역할 정보 설정
            ChannelAuthorities authority = channelAuthoritiesRepo.findByChannelIdAndUserId(
                    response.getId(), userId);
            if (authority != null) {
                response.setMyRole(authority.getRoleId());
            }
        } else {
            // 채널 생성자인지 확인
            Channels channel = channelsRepo.findById(response.getId()).orElse(null);
            if (channel != null && channel.getCreatedUserId().equals(userId)) {
                response.setIsMember(true);
                response.setMyRole("OWNER");
                response.setIsMuted(false);
                response.setUnreadMessageCount(0);
            } else {
                response.setIsMember(false);
                response.setIsMuted(false);
                response.setUnreadMessageCount(0);
            }
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
        // ✅ 수정: pageRequest.getPage()를 사용해야 함
        int page = pageRequest.getPage() != null ? pageRequest.getPage() : 0;
        int size = pageRequest.getSize() != null ? pageRequest.getSize() : 10;
        String sortBy = pageRequest.getSortBy() != null ? pageRequest.getSortBy() : "createdAt";
        String sortDirection = pageRequest.getSortDirection() != null ? pageRequest.getSortDirection() : "desc";

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // ✅ 핵심 수정: 0이 아니라 pageRequest.getPage() 사용
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
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