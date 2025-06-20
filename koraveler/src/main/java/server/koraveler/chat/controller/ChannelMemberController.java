// 3. ChannelMemberController.java
package server.koraveler.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.chat.dto.response.ChannelMemberResponse;
import server.koraveler.chat.model.enums.MemberStatus;
import server.koraveler.chat.model.enums.NotificationLevel;
import server.koraveler.chat.service.ChannelMemberService;
import server.koraveler.users.dto.CustomUserDetails;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels/{channelId}/members")
public class ChannelMemberController {

    private final ChannelMemberService channelMemberService;

    @PostMapping("/{userId}")
    public ResponseEntity<ChannelMemberResponse> addMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Adding member {} to channel {} by {}", userId, channelId, userDetails.getUsername());
        ChannelMemberResponse response = channelMemberService.addMember(
                channelId, userId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Removing member {} from channel {} by {}", userId, channelId, userDetails.getUsername());
        channelMemberService.removeMember(channelId, userId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ChannelMemberResponse>> getChannelMembers(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ChannelMemberResponse> members = channelMemberService.getChannelMembers(
                channelId, userDetails.getUsername());
        return ResponseEntity.ok(members);
    }

    @GetMapping("/online")
    public ResponseEntity<List<ChannelMemberResponse>> getOnlineMembers(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ChannelMemberResponse> onlineMembers = channelMemberService.getOnlineMembers(
                channelId, userDetails.getUsername());
        return ResponseEntity.ok(onlineMembers);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ChannelMemberResponse> updateMemberStatus(
            @PathVariable String channelId,
            @PathVariable String userId,
            @RequestParam MemberStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.updateMemberStatus(
                channelId, userId, status, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<ChannelMemberResponse> updateMemberRole(
            @PathVariable String channelId,
            @PathVariable String userId,
            @RequestParam String roleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.updateMemberRole(
                channelId, userId, roleId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/my-nickname")
    public ResponseEntity<ChannelMemberResponse> updateMyNickname(
            @PathVariable String channelId,
            @RequestParam String nickname,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.updateMemberNickname(
                channelId, nickname, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/my-notifications")
    public ResponseEntity<ChannelMemberResponse> updateNotificationSettings(
            @PathVariable String channelId,
            @RequestParam NotificationLevel level,
            @RequestParam Boolean enabled,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.updateNotificationSettings(
                channelId, level, enabled, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/mute")
    public ResponseEntity<ChannelMemberResponse> muteMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @RequestParam Long muteMinutes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.muteMember(
                channelId, userId, muteMinutes, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/mute")
    public ResponseEntity<ChannelMemberResponse> unmuteMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.unmuteMember(
                channelId, userId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/ban")
    public ResponseEntity<ChannelMemberResponse> banMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.banMember(
                channelId, userId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/ban")
    public ResponseEntity<ChannelMemberResponse> unbanMember(
            @PathVariable String channelId,
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChannelMemberResponse response = channelMemberService.unbanMember(
                channelId, userId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/permissions/{permission}")
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable String channelId,
            @PathVariable String userId,
            @PathVariable String permission) {

        boolean hasPermission = channelMemberService.hasPermission(channelId, userId, permission);
        return ResponseEntity.ok(hasPermission);
    }
}