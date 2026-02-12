package server.koraveler.travel.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import server.koraveler.error.CustomException;
import server.koraveler.error.ErrorCode;
import server.koraveler.travel.model.dto.*;
import server.koraveler.travel.model.embedded.TravelSchedule;
import server.koraveler.travel.model.entities.TravelMedia;
import server.koraveler.travel.model.entities.TravelUsers;
import server.koraveler.travel.model.entities.Travels;
import server.koraveler.travel.model.enums.TravelRole;
import server.koraveler.travel.model.enums.TravelVisibility;
import server.koraveler.travel.model.mapper.TravelMapper;
import server.koraveler.travel.repo.TravelMediaRepo;
import server.koraveler.travel.repo.TravelUsersRepo;
import server.koraveler.travel.repo.TravelsRepo;
import server.koraveler.travel.service.S3Service;
import server.koraveler.travel.service.TravelService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelServiceImpl implements TravelService {

    private final TravelsRepo travelsRepo;
    private final TravelUsersRepo travelUsersRepo;
    private final TravelMediaRepo travelMediaRepo;
    private final TravelMapper travelMapper;
    private final S3Service s3Service;

    // ==================== Travel CRUD ====================

    @Override
    public TravelResponse createTravel(TravelCreateRequest request, String userId) {
        log.info("Creating travel project for user: {}", userId);

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new CustomException(ErrorCode.INVALID_TRAVEL_DATE);
        }

        Travels travel = travelMapper.toEntity(request, userId);
        Travels savedTravel = travelsRepo.save(travel);

        // 생성자를 ADMIN으로 등록
        TravelUsers adminUser = TravelUsers.builder()
                .travelId(savedTravel.getId())
                .userId(userId)
                .role(TravelRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();
        adminUser.setCreated(LocalDateTime.now());
        adminUser.setUpdated(LocalDateTime.now());
        adminUser.setCreatedUser(userId);
        adminUser.setUpdatedUser(userId);
        travelUsersRepo.save(adminUser);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(savedTravel.getId());
        log.info("Travel project created: {}", savedTravel.getId());
        return travelMapper.toResponse(savedTravel, members);
    }

    @Override
    public TravelResponse getTravel(String travelId, String userId) {
        Travels travel = findTravelById(travelId);
        validateTravelAccess(travel, userId);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(travel, members);
    }

    @Override
    public TravelResponse updateTravel(String travelId, TravelUpdateRequest request, String userId) {
        log.info("Updating travel: {} by user: {}", travelId, userId);

        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        if (request.getTitle() != null) travel.setTitle(request.getTitle());
        if (request.getDescription() != null) travel.setDescription(request.getDescription());
        if (request.getCoverImageUrl() != null) travel.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getVisibility() != null) travel.setVisibility(request.getVisibility());
        if (request.getStatus() != null) travel.setStatus(request.getStatus());
        if (request.getStartDate() != null) travel.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) travel.setEndDate(request.getEndDate());
        if (request.getDestination() != null) travel.setDestination(request.getDestination());
        if (request.getTags() != null) travel.setTags(request.getTags());

        if (travel.getStartDate() != null && travel.getEndDate() != null
                && travel.getStartDate().isAfter(travel.getEndDate())) {
            throw new CustomException(ErrorCode.INVALID_TRAVEL_DATE);
        }

        travel.setUpdated(LocalDateTime.now());
        travel.setUpdatedUser(userId);
        Travels updatedTravel = travelsRepo.save(travel);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(updatedTravel, members);
    }

    @Override
    public void deleteTravel(String travelId, String userId) {
        log.info("Deleting travel: {} by user: {}", travelId, userId);

        findTravelById(travelId);
        validateAdminRole(travelId, userId);

        // 관련 데이터 삭제
        travelUsersRepo.deleteByTravelId(travelId);
        travelMediaRepo.deleteByTravelId(travelId);
        travelsRepo.deleteById(travelId);

        log.info("Travel project deleted: {}", travelId);
    }

    // ==================== Travel List ====================

    @Override
    public TravelListResponse getMyTravels(String userId, int page, int size) {
        List<TravelUsers> myMemberships = travelUsersRepo.findByUserId(userId);
        List<String> travelIds = myMemberships.stream().map(TravelUsers::getTravelId).toList();

        if (travelIds.isEmpty()) {
            return TravelListResponse.builder()
                    .travels(new ArrayList<>())
                    .pagination(TravelListResponse.PaginationInfo.builder()
                            .totalCount(0L)
                            .pageSize(size)
                            .currentPage(page)
                            .hasMore(false)
                            .build())
                    .build();
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created"));
        Page<Travels> travelPage = travelsRepo.findByIdIn(travelIds, pageRequest);

        List<TravelResponse> responses = travelPage.getContent().stream()
                .map(travel -> {
                    List<TravelUsers> members = travelUsersRepo.findByTravelId(travel.getId());
                    return travelMapper.toResponse(travel, members);
                })
                .toList();

        return TravelListResponse.builder()
                .travels(responses)
                .pagination(TravelListResponse.PaginationInfo.builder()
                        .totalCount(travelPage.getTotalElements())
                        .pageSize(size)
                        .currentPage(page)
                        .hasMore(travelPage.hasNext())
                        .build())
                .build();
    }

    @Override
    public TravelListResponse getPublicTravels(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created"));
        Page<Travels> travelPage = travelsRepo.findByVisibility(TravelVisibility.PUBLIC, pageRequest);

        List<TravelResponse> responses = travelPage.getContent().stream()
                .map(travel -> {
                    List<TravelUsers> members = travelUsersRepo.findByTravelId(travel.getId());
                    return travelMapper.toResponse(travel, members);
                })
                .toList();

        return TravelListResponse.builder()
                .travels(responses)
                .pagination(TravelListResponse.PaginationInfo.builder()
                        .totalCount(travelPage.getTotalElements())
                        .pageSize(size)
                        .currentPage(page)
                        .hasMore(travelPage.hasNext())
                        .build())
                .build();
    }

    // ==================== Member Management ====================

    @Override
    public TravelResponse addMember(String travelId, TravelMemberRequest request, String userId) {
        log.info("Adding member {} to travel {} by user {}", request.getUserId(), travelId, userId);

        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        if (travelUsersRepo.existsByTravelIdAndUserId(travelId, request.getUserId())) {
            throw new CustomException(ErrorCode.ALREADY_TRAVEL_MEMBER);
        }

        TravelUsers newMember = TravelUsers.builder()
                .travelId(travelId)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : TravelRole.USER)
                .nickname(request.getNickname())
                .joinedAt(LocalDateTime.now())
                .build();
        newMember.setCreated(LocalDateTime.now());
        newMember.setUpdated(LocalDateTime.now());
        newMember.setCreatedUser(userId);
        newMember.setUpdatedUser(userId);
        travelUsersRepo.save(newMember);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(travel, members);
    }

    @Override
    public void removeMember(String travelId, String targetUserId, String userId) {
        log.info("Removing member {} from travel {} by user {}", targetUserId, travelId, userId);

        findTravelById(travelId);
        validateAdminRole(travelId, userId);

        if (!travelUsersRepo.existsByTravelIdAndUserId(travelId, targetUserId)) {
            throw new CustomException(ErrorCode.NOT_TRAVEL_MEMBER);
        }

        // 마지막 ADMIN 삭제 방지
        TravelUsers targetMember = travelUsersRepo.findByTravelIdAndUserId(travelId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TRAVEL_MEMBER));

        if (targetMember.getRole() == TravelRole.ADMIN) {
            Long adminCount = travelUsersRepo.countByTravelIdAndRole(travelId, TravelRole.ADMIN);
            if (adminCount <= 1) {
                throw new CustomException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN);
            }
        }

        travelUsersRepo.deleteByTravelIdAndUserId(travelId, targetUserId);
    }

    @Override
    public TravelResponse updateMemberRole(String travelId, String targetUserId, TravelRole role, String userId) {
        log.info("Updating role of member {} in travel {} to {} by user {}", targetUserId, travelId, role, userId);

        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        TravelUsers targetMember = travelUsersRepo.findByTravelIdAndUserId(travelId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TRAVEL_MEMBER));

        // ADMIN -> USER 변경 시 마지막 ADMIN 체크
        if (targetMember.getRole() == TravelRole.ADMIN && role == TravelRole.USER) {
            Long adminCount = travelUsersRepo.countByTravelIdAndRole(travelId, TravelRole.ADMIN);
            if (adminCount <= 1) {
                throw new CustomException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN);
            }
        }

        targetMember.setRole(role);
        targetMember.setUpdated(LocalDateTime.now());
        targetMember.setUpdatedUser(userId);
        travelUsersRepo.save(targetMember);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(travel, members);
    }

    // ==================== Schedule Management ====================

    @Override
    public TravelResponse addSchedule(String travelId, TravelScheduleRequest request, String userId) {
        Travels travel = findTravelById(travelId);
        validateMembership(travelId, userId);

        TravelSchedule schedule = TravelSchedule.builder()
                .id(UUID.randomUUID().toString())
                .dayNumber(request.getDayNumber())
                .date(request.getDate())
                .title(request.getTitle())
                .description(request.getDescription())
                .places(request.getPlaces() != null ? request.getPlaces() : new ArrayList<>())
                .sortOrder(request.getSortOrder())
                .build();

        travel.getSchedules().add(schedule);
        travel.setUpdated(LocalDateTime.now());
        travel.setUpdatedUser(userId);
        Travels updatedTravel = travelsRepo.save(travel);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(updatedTravel, members);
    }

    @Override
    public TravelResponse updateSchedule(String travelId, String scheduleId, TravelScheduleRequest request, String userId) {
        Travels travel = findTravelById(travelId);
        validateMembership(travelId, userId);

        TravelSchedule schedule = travel.getSchedules().stream()
                .filter(s -> s.getId().equals(scheduleId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_SCHEDULE_NOT_FOUND));

        if (request.getTitle() != null) schedule.setTitle(request.getTitle());
        if (request.getDayNumber() != null) schedule.setDayNumber(request.getDayNumber());
        if (request.getDate() != null) schedule.setDate(request.getDate());
        if (request.getDescription() != null) schedule.setDescription(request.getDescription());
        if (request.getPlaces() != null) schedule.setPlaces(request.getPlaces());
        if (request.getSortOrder() != null) schedule.setSortOrder(request.getSortOrder());

        travel.setUpdated(LocalDateTime.now());
        travel.setUpdatedUser(userId);
        Travels updatedTravel = travelsRepo.save(travel);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(updatedTravel, members);
    }

    @Override
    public TravelResponse deleteSchedule(String travelId, String scheduleId, String userId) {
        Travels travel = findTravelById(travelId);
        validateMembership(travelId, userId);

        boolean removed = travel.getSchedules().removeIf(s -> s.getId().equals(scheduleId));
        if (!removed) {
            throw new CustomException(ErrorCode.TRAVEL_SCHEDULE_NOT_FOUND);
        }

        travel.setUpdated(LocalDateTime.now());
        travel.setUpdatedUser(userId);
        Travels updatedTravel = travelsRepo.save(travel);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(updatedTravel, members);
    }

    // ==================== Channel Management ====================

    @Override
    public TravelResponse linkChannel(String travelId, String channelId, String userId) {
        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        if (!travel.getChannelIds().contains(channelId)) {
            travel.getChannelIds().add(channelId);
            travel.setUpdated(LocalDateTime.now());
            travel.setUpdatedUser(userId);
            travelsRepo.save(travel);
        }

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(travel, members);
    }

    @Override
    public TravelResponse unlinkChannel(String travelId, String channelId, String userId) {
        Travels travel = findTravelById(travelId);
        validateAdminRole(travelId, userId);

        travel.getChannelIds().remove(channelId);
        travel.setUpdated(LocalDateTime.now());
        travel.setUpdatedUser(userId);
        travelsRepo.save(travel);

        List<TravelUsers> members = travelUsersRepo.findByTravelId(travelId);
        return travelMapper.toResponse(travel, members);
    }

    // ==================== Media Management ====================

    @Override
    public TravelMedia uploadMedia(String travelId, MultipartFile file, TravelMediaRequest request, String userId) {
        log.info("Uploading media to travel: {} by user: {}", travelId, userId);

        findTravelById(travelId);
        validateMembership(travelId, userId);

        String fileUrl = s3Service.uploadFile(file, travelId);
        String thumbnailUrl = s3Service.buildThumbnailUrl(fileUrl);

        TravelMedia media = TravelMedia.builder()
                .travelId(travelId)
                .uploadUserId(userId)
                .fileName(UUID.randomUUID().toString() + getExtension(file.getOriginalFilename()))
                .originalFileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .thumbnailUrl(thumbnailUrl)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .width(request != null ? request.getWidth() : null)
                .height(request != null ? request.getHeight() : null)
                .duration(request != null ? request.getDuration() : null)
                .description(request != null ? request.getDescription() : null)
                .takenAt(request != null ? request.getTakenAt() : null)
                .build();
        media.setCreated(LocalDateTime.now());
        media.setUpdated(LocalDateTime.now());
        media.setCreatedUser(userId);
        media.setUpdatedUser(userId);

        return travelMediaRepo.save(media);
    }

    @Override
    public List<TravelMedia> getMediaList(String travelId, String userId, int page, int size) {
        Travels travel = findTravelById(travelId);
        validateTravelAccess(travel, userId);

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<TravelMedia> mediaPage = travelMediaRepo.findByTravelIdOrderByCreatedDesc(travelId, pageRequest);
        List<TravelMedia> mediaList = mediaPage.getContent();

        // thumbnailUrl이 없거나 잘못된 경우 재계산 및 DB 업데이트
        List<TravelMedia> toUpdate = new ArrayList<>();
        for (TravelMedia media : mediaList) {
            if (media.getFileUrl() != null) {
                String correctUrl = s3Service.buildThumbnailUrl(media.getFileUrl());
                if (correctUrl != null && !correctUrl.equals(media.getThumbnailUrl())) {
                    media.setThumbnailUrl(correctUrl);
                    toUpdate.add(media);
                }
            }
        }
        if (!toUpdate.isEmpty()) {
            travelMediaRepo.saveAll(toUpdate);
        }

        return mediaList;
    }

    @Override
    public void deleteMedia(String travelId, String mediaId, String userId) {
        log.info("Deleting media: {} from travel: {} by user: {}", mediaId, travelId, userId);

        findTravelById(travelId);
        validateMembership(travelId, userId);

        TravelMedia media = travelMediaRepo.findById(mediaId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_MEDIA_NOT_FOUND));

        // 본인 업로드 또는 ADMIN만 삭제 가능
        if (!media.getUploadUserId().equals(userId)) {
            validateAdminRole(travelId, userId);
        }

        s3Service.deleteFile(media.getFileUrl());
        // 썸네일도 함께 삭제
        if (media.getThumbnailUrl() != null) {
            s3Service.deleteFile(media.getThumbnailUrl());
        }
        travelMediaRepo.deleteById(mediaId);
    }

    // ==================== Media Download ====================

    @Override
    public Resource downloadMedia(String travelId, String mediaId, String userId) {
        log.info("Downloading media: {} from travel: {} by user: {}", mediaId, travelId, userId);

        Travels travel = findTravelById(travelId);
        validateTravelAccess(travel, userId);

        TravelMedia media = travelMediaRepo.findById(mediaId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_MEDIA_NOT_FOUND));

        ResponseInputStream<GetObjectResponse> s3Object = s3Service.downloadFile(media.getFileUrl());
        return new InputStreamResource(s3Object);
    }

    @Override
    public TravelMedia getMediaInfo(String travelId, String mediaId, String userId) {
        Travels travel = findTravelById(travelId);
        validateTravelAccess(travel, userId);

        return travelMediaRepo.findById(mediaId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_MEDIA_NOT_FOUND));
    }

    @Override
    public Resource downloadMediaBatch(String travelId, List<String> mediaIds, String userId) {
        log.info("Batch downloading {} media from travel: {} by user: {}", mediaIds.size(), travelId, userId);

        Travels travel = findTravelById(travelId);
        validateTravelAccess(travel, userId);

        List<TravelMedia> mediaList = travelMediaRepo.findAllById(mediaIds);
        if (mediaList.isEmpty()) {
            throw new CustomException(ErrorCode.TRAVEL_MEDIA_NOT_FOUND);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (TravelMedia media : mediaList) {
                try (ResponseInputStream<GetObjectResponse> s3Object = s3Service.downloadFile(media.getFileUrl())) {
                    String entryName = media.getOriginalFileName() != null
                            ? media.getOriginalFileName()
                            : media.getFileName();
                    zos.putNextEntry(new ZipEntry(entryName));
                    s3Object.transferTo(zos);
                    zos.closeEntry();
                }
            }

            zos.finish();
            byte[] zipBytes = baos.toByteArray();
            return new org.springframework.core.io.ByteArrayResource(zipBytes);
        } catch (IOException e) {
            log.error("Failed to create ZIP for batch download: {}", e.getMessage());
            throw new CustomException(ErrorCode.S3_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private Travels findTravelById(String travelId) {
        return travelsRepo.findById(travelId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRAVEL_NOT_FOUND));
    }

    private void validateTravelAccess(Travels travel, String userId) {
        if (travel.getVisibility() == TravelVisibility.PUBLIC) {
            return;
        }
        if (!travelUsersRepo.existsByTravelIdAndUserId(travel.getId(), userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TRAVEL_ACCESS);
        }
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

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
