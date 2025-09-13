package server.koraveler.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import server.koraveler.chat.model.entities.Messages;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MessagesRepo extends MongoRepository<Messages, String> {

    // 클라이언트 ID와 사용자 ID로 중복 메시지 확인
    Optional<Messages> findByClientIdAndUserId(String clientId, String userId);

    // 채널별 메시지 조회 (페이징, 최신순)
    Page<Messages> findByChannelIdAndIsDeletedFalseOrderByCreatedAtAsc(
            String channelId,
            Pageable pageable
    );

    // 채널별 메시지 검색 (키워드 포함, 페이징)
    Page<Messages> findByChannelIdAndIsDeletedFalseAndMessageContainingIgnoreCaseOrderByCreatedAtDesc(
            String channelId,
            String keyword,
            Pageable pageable
    );

    // 채널별 메시지 수
    Long countByChannelIdAndIsDeletedFalse(String channelId);

    // 채널의 마지막 메시지 조회
    Optional<Messages> findFirstByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(String channelId);

    // 답글 조회 (오래된 순)
    Page<Messages> findByParentMessageIdAndIsDeletedFalseOrderByCreatedAtAsc(
            String parentMessageId,
            Pageable pageable
    );

    // 답글 수 조회
    Long countByParentMessageIdAndIsDeletedFalse(String parentMessageId);

    // 멘션된 메시지 조회 (최신순)
    Page<Messages> findByMentionedUserIdsContainingAndIsDeletedFalseOrderByCreatedAtDesc(
            String userId,
            Pageable pageable
    );

    // 멘션된 메시지 수 조회
    Long countByMentionedUserIdsContainingAndIsDeletedFalse(String userId);

    // 특정 시간 이후 메시지 수 (안 읽은 메시지)
    Long countByChannelIdAndCreatedAtAfterAndIsDeletedFalse(
            String channelId,
            LocalDateTime createdAt
    );

    // 채널의 마지막 메시지 정보 업데이트 (MongoDB @Query 사용)
    @Query("{'_id': ?0}")
    @Update("{'$set': {'lastMessageId': ?1, 'lastMessageAt': ?2, 'updatedAt': ?3}}")
    void updateLastMessage(String channelId, String messageId, LocalDateTime lastMessageAt, LocalDateTime updatedAt);
}