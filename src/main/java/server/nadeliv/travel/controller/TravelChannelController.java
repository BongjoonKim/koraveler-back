package server.nadeliv.travel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.travel.model.dto.TravelChannelCreateRequest;
import server.nadeliv.travel.model.dto.TravelChannelMemberResponse;
import server.nadeliv.travel.model.dto.TravelChannelResponse;
import server.nadeliv.travel.model.dto.TravelChannelUpdateRequest;
import server.nadeliv.travel.service.TravelChannelService;
import server.nadeliv.users.dto.CustomUserDetails;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/travels/{travelId}/channels")
public class TravelChannelController {

    private final TravelChannelService travelChannelService;

    // 채널 생성 (Travel 프로젝트에 새 채팅 채널 연결)
    @PostMapping
    public ResponseEntity<TravelChannelResponse> createChannel(
            @PathVariable String travelId,
            @Valid @RequestBody TravelChannelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TravelChannelResponse response = travelChannelService.createChannel(travelId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    // 채널 목록 조회 (Travel 프로젝트의 모든 연결된 채널)
    @GetMapping
    public ResponseEntity<List<TravelChannelResponse>> getChannels(
            @PathVariable String travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TravelChannelResponse> responses = travelChannelService.getChannels(travelId, userDetails.getUsername());
        return ResponseEntity.ok(responses);
    }

    // 단일 채널 조회
    @GetMapping("/{travelChannelId}")
    public ResponseEntity<TravelChannelResponse> getChannel(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TravelChannelResponse response = travelChannelService.getChannel(travelId, travelChannelId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    // 채널 설정 수정 (컨텍스트, 정렬, 고정 등)
    @PutMapping("/{travelChannelId}")
    public ResponseEntity<TravelChannelResponse> updateChannel(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @Valid @RequestBody TravelChannelUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TravelChannelResponse response = travelChannelService.updateChannel(travelId, travelChannelId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    // 채널 삭제 (브릿지 soft delete + 실제 채널 아카이브)
    @DeleteMapping("/{travelChannelId}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelChannelService.deleteChannel(travelId, travelChannelId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Travel 멤버 → 채널 멤버 동기화
    @PostMapping("/{travelChannelId}/sync")
    public ResponseEntity<Void> syncMembers(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelChannelService.syncMembers(travelId, travelChannelId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // 채널에 멤버 추가 (Travel 프로젝트 멤버만 허용)
    @PostMapping("/{travelChannelId}/members/{targetUserId}")
    public ResponseEntity<Void> addChannelMember(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @PathVariable String targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelChannelService.addChannelMember(travelId, travelChannelId, targetUserId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // 채널에서 멤버 제거
    @DeleteMapping("/{travelChannelId}/members/{targetUserId}")
    public ResponseEntity<Void> removeChannelMember(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @PathVariable String targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelChannelService.removeChannelMember(travelId, travelChannelId, targetUserId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // 채널에 추가 가능한 Travel 멤버 목록 조회
    @GetMapping("/{travelChannelId}/available-members")
    public ResponseEntity<List<TravelChannelMemberResponse>> getAvailableMembers(
            @PathVariable String travelId,
            @PathVariable String travelChannelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TravelChannelMemberResponse> members = travelChannelService.getAvailableMembers(
                travelId, travelChannelId, userDetails.getUsername());
        return ResponseEntity.ok(members);
    }
}
