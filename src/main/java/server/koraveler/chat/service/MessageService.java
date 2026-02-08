// 1. MessageService.java
package server.koraveler.chat.service;

import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MessageService {

    // 메시지 생성
    MessageResponse createMessage(MessageCreateRequest request, String userId);

    // 메시지 수정
    MessageResponse updateMessage(String messageId, MessageUpdateRequest request, String userId);

    // 메시지 삭제 (소프트 삭제)
    void deleteMessage(String messageId, String userId);

    // 메시지 조회
    MessageResponse getMessage(String messageId, String userId);

    // 채널별 메시지 목록 조회 (페이징)
    MessageListResponse getChannelMessages(String channelId, PageRequest pageRequest, String userId);

    // 메시지 검색
    MessageListResponse searchMessages(String channelId, MessageSearchRequest searchRequest, String userId);

    // 메시지 반응 추가/제거
    MessageResponse addReaction(String messageId, MessageReactionRequest request, String userId);

    // 메시지 고정/고정 해제
    MessageResponse pinMessage(String messageId, String userId);
    MessageResponse unpinMessage(String messageId, String userId);

    // 메시지 읽음 처리
    void markAsRead(String channelId, String messageId, String userId);
    void markChannelAsRead(String channelId, String userId);

    // 답글 조회
    MessageListResponse getReplies(String parentMessageId, PageRequest pageRequest, String userId);

    // 멘션된 메시지 조회
    MessageListResponse getMentionedMessages(String userId, PageRequest pageRequest);

    // 안 읽은 메시지 수 조회
    Integer getUnreadMessageCount(String channelId, String userId);
}