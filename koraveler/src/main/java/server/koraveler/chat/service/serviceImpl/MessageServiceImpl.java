// 1. MessageServiceImpl.java
package server.koraveler.chat.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.koraveler.chat.dto.request.*;
import server.koraveler.chat.dto.response.*;
import server.koraveler.chat.dto.mapper.MessageMapper;
import server.koraveler.chat.model.entities.Messages;
import server.koraveler.chat.model.entities.ChannelMembers;
import server.koraveler.chat.model.enums.MessageStatus;
import server.koraveler.chat.repository.MessagesRepo;
import server.koraveler.chat.repository.ChannelMembersRepo;
import server.koraveler.chat.repository.ChannelsRepo;
import server.koraveler.chat.service.MessageService;
import server.koraveler.chat.exception.CustomException;
import server.koraveler.chat.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessagesRepo messagesRepo;
    private final ChannelMembersRepo channelMembersRepo;
    private final ChannelsRepo channelsRepo;
    private final MessageMapper messageMapper;

    @Override
    public MessageResponse createMessage(MessageCreateRequest request, String userId) {
        log.info("Creating message for user: {} in channel: {}", userId, request.getChannelId());

        // 채널 멤버인지 확인
        validateChannelMembership(request.getChannelId(), userId);

        // 중복 메시지 체크 (clientId 기반)
        if (request.getClientId() != null) {
            Optional<Messages> existingMessage = messagesRepo
                    .findByClientIdAndUserId(request.getClientId(), userId);
            if (existingMessage.isPresent()) {
                return messageMapper.toResponse(existingMessage.get());
            }
        }

        // 메시지 생성
        Messages message = messageMapper.toEntity(request, userId);
        Messages savedMessage = messagesRepo.save(message);

        // 채널의 마지막 메시지 정보 업데이트
        updateChannelLastMessage(request.getChannelId(), savedMessage.getId());

        // 멘션된 사용자들에게 알림 처리 (비동기)
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            processMentionNotifications(savedMessage, request.getMentionedUserIds());
        }

        log.info("Message created successfully: {}", savedMessage.getId());
        return messageMapper.toResponse(savedMessage);
    }

    @Override
    public MessageResponse updateMessage(String messageId, MessageUpdateRequest request, String userId) {
        log.info("Updating message: {} by user: {}", messageId, userId);

        Messages message = messagesRepo.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 메시지 작성자인지 확인
        if (!message.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MESSAGE_ACCESS);
        }

        // 메시지 수정
        message.setMessage(request.getMessage());
        message.setIsEdited(true);
        message.setUpdatedAt(LocalDateTime.now());

        Messages updatedMessage = messagesRepo.save(message);

        log.info("Message updated successfully: {}", messageId);
        return messageMapper.toResponse(updatedMessage);
    }

    @Override
    public void deleteMessage(String messageId, String userId) {
        log.info("Deleting message: {} by user: {}", messageId, userId);

        Messages message = messagesRepo.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 삭제 권한 확인 (작성자 또는 채널 관리자)
        if (!message.getUserId().equals(userId) && !hasChannelDeletePermission(message.getChannelId(), userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MESSAGE_DELETE);
        }

        // 소프트 삭제
        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setStatus(MessageStatus.DELETED);

        messagesRepo.save(message);
        log.info("Message deleted successfully: {}", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getMessage(String messageId, String userId) {
        Messages message = messagesRepo.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 채널 접근 권한 확인
        validateChannelAccess(message.getChannelId(), userId);

        return messageMapper.toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageListResponse getChannelMessages(String channelId, PageRequest pageRequest, String userId) {
        log.info("Getting messages for channel: {} by user: {}", channelId, userId);

        // 채널 접근 권한 확인
        validateChannelAccess(channelId, userId);

        // 페이징된 메시지 조회
        List<Messages> messages = messagesRepo.findByChannelIdAndIsDeletedFalse(
                channelId, pageRequest.getSize(), pageRequest.getCursor());

        List<MessageResponse> messageResponses = messages.stream()
                .map(messageMapper::toResponse)
                .toList();

        // 다음 페이지 존재 여부 확인
        boolean hasNext = messages.size() == pageRequest.getSize();
        String nextCursor = hasNext && !messages.isEmpty() ?
                messages.get(messages.size() - 1).getId() : null;

        return MessageListResponse.builder()
                .messages(messageResponses)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .totalCount(messagesRepo.countByChannelIdAndIsDeletedFalse(channelId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MessageListResponse searchMessages(String channelId, MessageSearchRequest searchRequest, String userId) {
        // 채널 접근 권한 확인
        validateChannelAccess(channelId, userId);

        // 검색 실행
        List<Messages> messages = messagesRepo.searchMessages(channelId, searchRequest);

        List<MessageResponse> messageResponses = messages.stream()
                .map(messageMapper::toResponse)
                .toList();

        return MessageListResponse.builder()
                .messages(messageResponses)
                .hasNext(false) // 검색 결과는 일반적으로 페이징 없이 제공
                .nextCursor(null)
                .totalCount(messageResponses.size())
                .build();
    }

    @Override
    public MessageResponse addReaction(String messageId, MessageReactionRequest request, String userId) {
        Messages message = messagesRepo.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 채널 접근 권한 확인
        validateChannelAccess(message.getChannelId(), userId);

        // 반응 추가/제거 로직
        if (request.getIsAdd()) {
            addReactionToMessage(message, request.getEmoji(), userId);
        } else {
            removeReactionFromMessage(message, request.getEmoji(), userId);
        }

        Messages updatedMessage = messagesRepo.save(message);
        return messageMapper.toResponse(updatedMessage);
    }

    @Override
    public MessageResponse pinMessage(String messageId, String userId) {
        Messages message = messagesRepo.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 고정 권한 확인
        if (!hasChannelPinPermission(message.getChannelId(), userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PIN_MESSAGE);
        }

        message.setIsPinned(true);
        message.setUpdatedAt(LocalDateTime.now());

        Messages updatedMessage = messagesRepo.save(message);
        return messageMapper.toResponse(updatedMessage);
    }

    @Override
    public MessageResponse unpinMessage(String messageId, String userId) {
        Messages message = messagesRepo.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 고정 해제 권한 확인
        if (!hasChannelPinPermission(message.getChannelId(), userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PIN_MESSAGE);
        }

        message.setIsPinned(false);
        message.setUpdatedAt(LocalDateTime.now());

        Messages updatedMessage = messagesRepo.save(message);
        return messageMapper.toResponse(updatedMessage);
    }

    @Override
    public void markAsRead(String channelId, String messageId, String userId) {
        // 읽음 상태 업데이트
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        member.setLastReadMessageId(messageId);
        member.setLastSeenAt(LocalDateTime.now());
        channelMembersRepo.save(member);
    }

    @Override
    public void markChannelAsRead(String channelId, String userId) {
        // 채널의 모든 메시지를 읽음 처리
        String lastMessageId = messagesRepo.findLastMessageIdByChannelId(channelId);
        if (lastMessageId != null) {
            markAsRead(channelId, lastMessageId, userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MessageListResponse getReplies(String parentMessageId, PageRequest pageRequest, String userId) {
        Messages parentMessage = messagesRepo.findById(parentMessageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 채널 접근 권한 확인
        validateChannelAccess(parentMessage.getChannelId(), userId);

        List<Messages> replies = messagesRepo.findByParentMessageIdAndIsDeletedFalse(
                parentMessageId, pageRequest);

        List<MessageResponse> replyResponses = replies.stream()
                .map(messageMapper::toResponse)
                .toList();

        return MessageListResponse.builder()
                .messages(replyResponses)
                .hasNext(replies.size() == pageRequest.getSize())
                .totalCount(messagesRepo.countByParentMessageIdAndIsDeletedFalse(parentMessageId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MessageListResponse getMentionedMessages(String userId, PageRequest pageRequest) {
        List<Messages> mentionedMessages = messagesRepo.findByMentionedUserIdsContainingAndIsDeletedFalse(
                userId, pageRequest);

        List<MessageResponse> messageResponses = mentionedMessages.stream()
                .map(messageMapper::toResponse)
                .toList();

        return MessageListResponse.builder()
                .messages(messageResponses)
                .hasNext(mentionedMessages.size() == pageRequest.getSize())
                .totalCount(messagesRepo.countByMentionedUserIdsContainingAndIsDeletedFalse(userId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getUnreadMessageCount(String channelId, String userId) {
        ChannelMembers member = channelMembersRepo.findByChannelIdAndUserId(channelId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_CHANNEL_MEMBER));

        return messagesRepo.countUnreadMessages(channelId, member.getLastReadMessageId());
    }

    // ===== Private Helper Methods =====

    private void validateChannelMembership(String channelId, String userId) {
        if (!channelMembersRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new CustomException(ErrorCode.NOT_CHANNEL_MEMBER);
        }
    }

    private void validateChannelAccess(String channelId, String userId) {
        // 채널 존재 여부 및 접근 권한 확인
        if (!channelsRepo.existsById(channelId)) {
            throw new CustomException(ErrorCode.CHANNEL_NOT_FOUND);
        }
        validateChannelMembership(channelId, userId);
    }

    private void updateChannelLastMessage(String channelId, String messageId) {
        channelsRepo.updateLastMessage(channelId, messageId, LocalDateTime.now(), LocalDateTime.now());
    }

    private void processMentionNotifications(Messages message, List<String> mentionedUserIds) {
        // 비동기로 멘션 알림 처리
        // 실제 구현에서는 이벤트 발행 또는 메시지 큐 사용
        log.info("Processing mention notifications for message: {} to users: {}",
                message.getId(), mentionedUserIds);
    }

    private boolean hasChannelDeletePermission(String channelId, String userId) {
        // 채널에서 메시지 삭제 권한 확인 로직
        // 실제로는 ChannelAuthorities를 통해 권한 확인
        return true; // 임시
    }

    private boolean hasChannelPinPermission(String channelId, String userId) {
        // 채널에서 메시지 고정 권한 확인 로직
        return true; // 임시
    }

    private void addReactionToMessage(Messages message, String emoji, String userId) {
        // 메시지에 반응 추가 로직
        // MessageReaction 객체 조작
    }

    private void removeReactionFromMessage(Messages message, String emoji, String userId) {
        // 메시지에서 반응 제거 로직
        // MessageReaction 객체 조작
    }
}