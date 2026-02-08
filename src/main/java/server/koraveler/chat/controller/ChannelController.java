// 2. ChannelController.java
package server.koraveler.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import server.koraveler.chat.service.ChannelService;
import server.koraveler.users.dto.CustomUserDetails;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(
            @Valid @RequestBody ChannelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Creating channel: {} by user: {}", request.getName(), userDetails.getUsername());
        ChannelResponse response = channelService.createChannel(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{channelId}")
    public ResponseEntity<ChannelResponse> updateChannel(
            @PathVariable String channelId,
            @Valid @RequestBody ChannelUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Updating channel: {} by user: {}", channelId, userDetails.getUsername());
        ChannelResponse response = channelService.updateChannel(channelId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Deleting channel: {} by user: {}", channelId, userDetails.getUsername());
        channelService.deleteChannel(channelId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{channelId}/archive")
    public ResponseEntity<Void> archiveChannel(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        channelService.archiveChannel(channelId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{channelId}/unarchive")
    public ResponseEntity<Void> unarchiveChannel(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        channelService.unarchiveChannel(channelId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelResponse> getChannel(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelResponse response = channelService.getChannel(channelId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-channels")
    public ResponseEntity<ChannelListResponse> getUserChannels(
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .cursor(cursor)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        ChannelListResponse response = channelService.getUserChannels(userDetails.getUsername(), pageRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<ChannelListResponse> getPublicChannels(
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .cursor(cursor)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        ChannelListResponse response = channelService.getPublicChannels(pageRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ChannelListResponse> searchChannels(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .cursor(cursor)
                .build();

        ChannelListResponse response = channelService.searchChannels(
                keyword, userDetails.getUsername(), pageRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{channelId}/join")
    public ResponseEntity<ChannelResponse> joinChannel(
            @PathVariable String channelId,
            @RequestBody(required = false) ChannelJoinRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (request == null) {
            request = new ChannelJoinRequest();
        }

        log.info("User {} joining channel: {}", userDetails.getUsername(), channelId);
        ChannelResponse response = channelService.joinChannel(channelId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{channelId}/leave")
    public ResponseEntity<Void> leaveChannel(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("User {} leaving channel: {}", userDetails.getUsername(), channelId);
        channelService.leaveChannel(channelId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{channelId}/settings")
    public ResponseEntity<ChannelResponse> updateChannelSettings(
            @PathVariable String channelId,
            @Valid @RequestBody ChannelSettingsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelResponse response = channelService.updateChannelSettings(
                channelId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{channelId}/invite-link")
    public ResponseEntity<String> generateInviteLink(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String inviteCode = channelService.generateInviteLink(channelId, userDetails.getUsername());
        return ResponseEntity.ok(inviteCode);
    }

    @PostMapping("/join-by-invite/{inviteCode}")
    public ResponseEntity<ChannelResponse> joinByInviteLink(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelResponse response = channelService.joinByInviteLink(inviteCode, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}