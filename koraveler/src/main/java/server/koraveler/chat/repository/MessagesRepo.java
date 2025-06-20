// 1. MessagesRepo.java
package server.koraveler.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.koraveler.chat.dto.request.MessageSearchRequest;
import server.koraveler.chat.dto.request.PageRequest;
import server.koraveler.chat.model.entities.Messages;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessagesRepo extends MongoRepository<Messages, String> {

    // 클라이언트 ID와 사용자 ID로 중복 메시지 확인
    Optional<Messages> findByClientIdAndUserId(String clientId, String userId);

    // 채널별 메시지 조회 (삭제되지 않은 것만, 페이징)
    @Query("{'channelId': ?0, 'isDeleted': false}")
    List<Messages> findByChannelIdAndIsDeletedFalse(String channelId, int limit, String cursor);

    // 채널별 메시지 수 (삭제되지 않은 것만)
    @Query(value = "{'channelId': ?0, 'isDeleted': false}", count = true)
    Integer countByChannelIdAndIsDeletedFalse(String channelId);

    // 채널의 마지막 메시지 ID 조회
    @Query("{'channelId': ?0, 'isDeleted': false}")
    String findLastMessageIdByChannelId(String channelId);

    // 답글 조회
    List<Messages> findByParentMessageIdAndIsDeletedFalse(String parentMessageId, PageRequest pageRequest);

    // 답글 수 조회
    @Query(value = "{'parentMessageId': ?0, 'isDeleted': false}", count = true)
    Integer countByParentMessageIdAndIsDeletedFalse(String parentMessageId);

    // 멘션된 메시지 조회
    @Query("{'mentionedUserIds': {$in: [?0]}, 'isDeleted': false}")
    List<Messages> findByMentionedUserIdsContainingAndIsDeletedFalse(String userId, PageRequest pageRequest);

    // 멘션된 메시지 수 조회
    @Query(value = "{'mentionedUserIds': {$in: [?0]}, 'isDeleted': false}", count = true)
    Integer countByMentionedUserIdsContainingAndIsDeletedFalse(String userId);

    // 안 읽은 메시지 수 계산
    @Query(value = "{'channelId': ?0, 'id': {$gt: ?1}, 'isDeleted': false}", count = true)
    Integer countUnreadMessages(String channelId, String lastReadMessageId);

    // 메시지 검색
    default List<Messages> searchMessages(String channelId, MessageSearchRequest searchRequest) {
        // 실제 구현에서는 MongoDB Aggregation Pipeline 사용
        // 여기서는 간단한 예시만 제공
        return List.of();
    }
}