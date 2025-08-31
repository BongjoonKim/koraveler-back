// ChatWebSocketController.java
package server.koraveler.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import server.koraveler.chat.dto.request.MessageCreateRequest;
import server.koraveler.chat.dto.response.MessageResponse;
import server.koraveler.chat.service.MessageService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{channelId}/send")
    public void sendMessage(
            @DestinationVariable String channelId,
            @Payload MessageCreateRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            // 헤더에서 사용자 정보 추출
            String userId = headerAccessor.getUser().getName();

            // 메시지 저장
            MessageResponse response = messageService.createMessage(request, userId);

            // 채널 구독자들에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/channel/" + channelId + "/messages",
                    response
            );

            log.info("Message sent to channel {}: {}", channelId, response.getId());

        } catch (Exception e) {
            log.error("Error sending message: ", e);
            // 에러를 보낸 사용자에게만 전송
            messagingTemplate.convertAndSendToUser(
                    headerAccessor.getUser().getName(),
                    "/queue/errors",
                    "메시지 전송에 실패했습니다: " + e.getMessage()
            );
        }
    }

    @MessageMapping("/chat/{channelId}/typing")
    public void handleTyping(
            @DestinationVariable String channelId,
            @Payload TypingEvent typingEvent,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userId = headerAccessor.getUser().getName();
        typingEvent.setUserId(userId);

        // 타이핑 이벤트를 다른 사용자들에게 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/channel/" + channelId + "/typing",
                typingEvent
        );
    }

    // 타이핑 이벤트 클래스
    public static class TypingEvent {
        private String userId;
        private boolean isTyping;

        // getters, setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public boolean isTyping() { return isTyping; }
        public void setTyping(boolean typing) { isTyping = typing; }
    }
}