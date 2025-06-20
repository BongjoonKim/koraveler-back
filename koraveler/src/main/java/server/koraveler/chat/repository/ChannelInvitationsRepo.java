// 5. ChannelInvitationsRepository.java
package server.koraveler.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.koraveler.chat.model.entities.ChannelInvitations;
import server.koraveler.chat.model.enums.InvitationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelInvitationsRepo extends MongoRepository<ChannelInvitations, String> {

    // 초대 코드로 채널 조회
    @Query("{'inviteCode': ?0, 'status': 'PENDING', 'expiresAt': {$gt: ?#{T(java.time.LocalDateTime).now()}}}")
    Optional<ChannelInvitations> findValidInvitation(String inviteCode);

    // 채널의 활성 초대 목록
    @Query("{'channelId': ?0, 'status': 'PENDING', 'expiresAt': {$gt: ?#{T(java.time.LocalDateTime).now()}}}")
    List<ChannelInvitations> findActiveInvitations(String channelId);

    // 만료된 초대 정리
    @Query("{'expiresAt': {$lt: ?0}}")
    void deleteExpiredInvitations(LocalDateTime now);
}