// ChannelsRepo.java
package server.koraveler.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import server.koraveler.chat.model.entities.Channels;
import server.koraveler.chat.model.enums.ChannelType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChannelsRepo extends MongoRepository<Channels, String> {

    // 채널명 중복 체크
    boolean existsByNameAndChannelType(String name, ChannelType channelType);

    // 공개 채널 목록 조회 (아카이브되지 않은 것만)
    @Query("{'channelType': 'PUBLIC', 'isArchived': false}")
    List<Channels> findPublicChannels(Pageable pageable);

    // 공개 채널 수 조회
    @Query(value = "{'channelType': 'PUBLIC', 'isArchived': false}", count = true)
    Long countPublicChannels();

    // 사용자 참여 채널 목록 (Aggregation Pipeline 사용)
    @Query(value = "{ $expr: { $and: [" +
            "{ $eq: ['$isArchived', false] }," +
            "{ $in: ['$_id', ?1] }" +
            "] } }")
    List<Channels> findUserChannelsByIds(String userId, List<String> channelIds, Pageable pageable);

    // 사용자 참여 채널 수
    @Query(value = "{ $expr: { $and: [" +
            "{ $eq: ['$isArchived', false] }," +
            "{ $in: ['$_id', ?1] }" +
            "] } }", count = true)
    Long countUserChannelsByIds(String userId, List<String> channelIds);

    // 채널 검색 - 공개 채널 대상
    @Query("{ $and: [" +
            "{ 'isArchived': false }," +
            "{ 'channelType': 'PUBLIC' }," +
            "{ $or: [" +
            "{ 'name': { $regex: ?0, $options: 'i' } }," +
            "{ 'description': { $regex: ?0, $options: 'i' } }," +
            "{ 'topic': { $regex: ?0, $options: 'i' } }," +
            "{ 'tags': { $in: [?0] } }" +
            "] }" +
            "] }")
    List<Channels> searchPublicChannels(String keyword, Pageable pageable);

    // 채널 검색 - 사용자 채널 대상
    @Query("{ $and: [" +
            "{ 'isArchived': false }," +
            "{ '_id': { $in: ?1 } }," +
            "{ $or: [" +
            "{ 'name': { $regex: ?0, $options: 'i' } }," +
            "{ 'description': { $regex: ?0, $options: 'i' } }," +
            "{ 'topic': { $regex: ?0, $options: 'i' } }," +
            "{ 'tags': { $in: [?0] } }" +
            "] }" +
            "] }")
    List<Channels> searchUserChannels(String keyword, List<String> channelIds, Pageable pageable);

    // 공개 채널 검색 결과 수
    @Query(value = "{ $and: [" +
            "{ 'isArchived': false }," +
            "{ 'channelType': 'PUBLIC' }," +
            "{ $or: [" +
            "{ 'name': { $regex: ?0, $options: 'i' } }," +
            "{ 'description': { $regex: ?0, $options: 'i' } }," +
            "{ 'topic': { $regex: ?0, $options: 'i' } }," +
            "{ 'tags': { $in: [?0] } }" +
            "] }" +
            "] }", count = true)
    Long countSearchPublicChannels(String keyword);

    // 사용자 채널 검색 결과 수
    @Query(value = "{ $and: [" +
            "{ 'isArchived': false }," +
            "{ '_id': { $in: ?1 } }," +
            "{ $or: [" +
            "{ 'name': { $regex: ?0, $options: 'i' } }," +
            "{ 'description': { $regex: ?0, $options: 'i' } }," +
            "{ 'topic': { $regex: ?0, $options: 'i' } }," +
            "{ 'tags': { $in: [?0] } }" +
            "] }" +
            "] }", count = true)
    Long countSearchUserChannels(String keyword, List<String> channelIds);

    // 마지막 메시지 정보 업데이트
    @Query("{'_id': ?0}")
    @Update("{'$set': {'lastMessageId': ?1, 'lastMessageAt': ?2, 'updatedAt': ?3}}")
    void updateLastMessage(String channelId, String messageId, LocalDateTime lastMessageAt, LocalDateTime updatedAt);

    // 멤버 수 업데이트
    @Query("{'_id': ?0}")
    @Update("{'$set': {'memberCount': ?1, 'updatedAt': ?2}}")
    void updateMemberCount(String channelId, Integer memberCount, LocalDateTime updatedAt);

    // Spring Data에서 제공하는 기본 메서드들
    List<Channels> findByChannelTypeAndIsArchivedFalse(ChannelType channelType);
    List<Channels> findByCreatedUserIdAndIsArchivedFalse(String createdUserId);

    // 채널 타입별 조회 (페이징)
    List<Channels> findByChannelTypeAndIsArchivedFalse(ChannelType channelType, Pageable pageable);

    // 최근 생성된 채널 조회
    List<Channels> findByIsArchivedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 활성 멤버 수가 많은 채널 조회
    List<Channels> findByIsArchivedFalseOrderByMemberCountDesc(Pageable pageable);
}