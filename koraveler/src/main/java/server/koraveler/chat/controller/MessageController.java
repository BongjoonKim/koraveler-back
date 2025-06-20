// 1. MessageController.java
package server.koraveler.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import server.koraveler.chat.service.MessageService;
import server.koraveler.users.dto.CustomUserDetails;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
            @Valid @RequestBody MessageCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Creating message for user: {}", userDetails.getUsername());
        MessageResponse response = messageService.createMessage(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable String messageId,
            @Valid @RequestBody MessageUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Updating message: {} by user: {}", messageId, userDetails.getUsername());
        MessageResponse response = messageService.updateMessage(messageId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Deleting message: {} by user: {}", messageId, userDetails.getUsername());
        messageService.deleteMessage(messageId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageResponse response = messageService.getMessage(messageId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/channels/{channelId}")
    public ResponseEntity<MessageListResponse> getChannelMessages(
            @PathVariable String channelId,
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

        MessageListResponse response = messageService.getChannelMessages(
                channelId, pageRequest, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<MessageListResponse> searchMessages(
            @RequestParam String channelId,
            @Valid @RequestBody MessageSearchRequest searchRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageListResponse response = messageService.searchMessages(
                channelId, searchRequest, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<MessageResponse> addReaction(
            @PathVariable String messageId,
            @Valid @RequestBody MessageReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageResponse response = messageService.addReaction(messageId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{messageId}/pin")
    public ResponseEntity<MessageResponse> pinMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageResponse response = messageService.pinMessage(messageId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}/pin")
    public ResponseEntity<MessageResponse> unpinMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageResponse response = messageService.unpinMessage(messageId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/channels/{channelId}/read")
    public ResponseEntity<Void> markChannelAsRead(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        messageService.markChannelAsRead(channelId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String messageId,
            @RequestParam String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        messageService.markAsRead(channelId, messageId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{parentMessageId}/replies")
    public ResponseEntity<MessageListResponse> getReplies(
            @PathVariable String parentMessageId,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .cursor(cursor)
                .build();

        MessageListResponse response = messageService.getReplies(
                parentMessageId, pageRequest, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mentions")
    public ResponseEntity<MessageListResponse> getMentionedMessages(
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest = PageRequest.builder()
                .size(size)
                .cursor(cursor)
                .build();

        MessageListResponse response = messageService.getMentionedMessages(
                userDetails.getUsername(), pageRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/channels/{channelId}/unread-count")
    public ResponseEntity<Integer> getUnreadMessageCount(
            @PathVariable String channelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Integer count = messageService.getUnreadMessageCount(channelId, userDetails.getUsername());
        return ResponseEntity.ok(count);
    }
}