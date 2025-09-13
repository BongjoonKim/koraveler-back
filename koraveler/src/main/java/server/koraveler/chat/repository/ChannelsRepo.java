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

    // ===== 간단한 테스트용 메서드들 =====

    // 1. 가장 기본적인 Spring Data 메서드 (이것부터 테스트)
    List<Channels> findByIdIn(List<String> ids);

    // 2. 페이징 포함
    List<Channels> findByIdIn(List<String> ids, Pageable pageable);

    // 3. isArchived 조건 추가 (null 처리 포함)
    List<Channels> findByIdInAndIsArchivedNot(List<String> ids, Boolean archived, Pageable pageable);

    // 4. 생성자로 조회
    List<Channels> findByCreatedUserId(String userId);

    // 5. 생성자 + isArchived 조건
    List<Channels> findByCreatedUserIdAndIsArchivedNot(String userId, Boolean archived);

    // 채널명 중복 체크
    boolean existsByNameAndChannelType(String name, ChannelType channelType);

    // 공개 채널 목록 조회 (아카이브되지 않은 것만)
    @Query("{'channelType': 'PUBLIC', 'isArchived': false}")
    List<Channels> findPublicChannels(Pageable pageable);

    // 공개 채널 수 조회
    @Query(value = "{'channelType': 'PUBLIC', 'isArchived': false}", count = true)
    Long countPublicChannels();

    // ✅ 수정된 쿼리 - userId 파라미터 제거하고 올바른 MongoDB 문법 사용
    @Query("{ $and: [" +
            "{ 'isArchived': false }," +
            "{ '_id': { $in: ?0 } }" +  // ?0은 첫 번째 파라미터인 channelIds
            "] }")
    List<Channels> findUserChannelsByIds(List<String> channelIds, Pageable pageable);

    // ✅ 수정된 카운트 쿼리
    @Query(value = "{ $and: [" +
            "{ 'isArchived': false }," +
            "{ '_id': { $in: ?0 } }" +
            "] }", count = true)
    Long countUserChannelsByIds(List<String> channelIds);

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

    // ✅ 수정된 사용자 채널 검색
    @Query("{ $and: [" +
            "{ 'isArchived': false }," +
            "{ '_id': { $in: ?1 } }," +  // ?1은 두 번째 파라미터인 channelIds
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

    // ✅ 수정된 사용자 채널 검색 카운트
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

    // ✅ 대안: Spring Data 메서드 네이밍 컨벤션 사용 (쿼리 없이)
    List<Channels> findByIdInAndIsArchivedFalse(List<String> ids, Pageable pageable);
}