package server.koraveler.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import server.koraveler.chat.model.entities.ChannelAuthorities;

@Repository
public interface ChannelAuthoritiesRepo extends MongoRepository<ChannelAuthorities, String> {

    // 사용자의 채널 권한 확인
    @Query("{'channelId': ?0, 'userId': ?1}")
    ChannelAuthorities findByChannelIdAndUserId(String channelId, String userId);

    // 특정 권한 보유 여부 확인
    @Query("{'channelId': ?0, 'userId': ?1, 'permissions': ?2}")
    boolean hasPermission(String channelId, String userId, String permission);

    // 멤버 역할 업데이트
    @Query("{'channelId': ?0, 'userId': ?1}")
    @Update("{'$set': {'roleId': ?2}}")
    void updateMemberRole(String channelId, String userId, String roleId);
}