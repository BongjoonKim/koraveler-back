// ChannelMembersRepo에 추가해야 할 메서드들

package server.koraveler.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.koraveler.chat.model.entities.ChannelMembers;
import server.koraveler.chat.model.enums.MemberStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMembersRepo extends MongoRepository<ChannelMembers, String> {

    // 기존 메서드들...
    boolean existsByChannelIdAndUserId(String channelId, String userId);
    Optional<ChannelMembers> findByChannelIdAndUserId(String channelId, String userId);
    List<ChannelMembers> findByChannelIdAndStatus(String channelId, MemberStatus status);

    // 누락된 메서드 추가
    List<ChannelMembers> findByUserIdAndStatus(String userId, MemberStatus status);

    // 활성 멤버인지 확인
    boolean existsByChannelIdAndUserIdAndStatus(String channelId, String userId, MemberStatus status);

    // 채널의 활성 멤버 수 조회
    @Query(value = "{'channelId': ?0, 'status': 'ACTIVE'}", count = true)
    Long countActiveMembers(String channelId);

    // ChannelMembersRepo.java
    List<ChannelMembers> findByChannelIdAndStatusAndLastSeenAtAfter(
            String channelId,
            MemberStatus status,
            LocalDateTime sinceTime
    );

    // 특정 사용자의 모든 채널 멤버십 조회
    List<ChannelMembers> findByUserId(String userId);

    // 채널의 모든 멤버 조회 (상태 관계없이)
    List<ChannelMembers> findByChannelId(String channelId);
}