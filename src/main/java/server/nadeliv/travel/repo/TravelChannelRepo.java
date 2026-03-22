package server.nadeliv.travel.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.nadeliv.travel.model.entities.TravelChannel;
import server.nadeliv.travel.model.enums.ChannelContextType;

import java.util.List;
import java.util.Optional;

public interface TravelChannelRepo extends MongoRepository<TravelChannel, String> {

    // 특정 Travel의 채널 목록 (삭제되지 않은 것만, 정렬 순서)
    // ⚠️ Boolean wrapper 타입은 IsDeletedFalse 사용 (primitive boolean은 DeletedFalse)
    List<TravelChannel> findByTravelIdAndIsDeletedFalseOrderByDisplayOrderAsc(String travelId);

    // 특정 Travel + contextType으로 필터
    List<TravelChannel> findByTravelIdAndContextTypeAndIsDeletedFalse(
            String travelId, ChannelContextType contextType);

    // 특정 channelId로 조회 (역방향 — 채널에서 Travel 찾기)
    Optional<TravelChannel> findByChannelIdAndIsDeletedFalse(String channelId);

    // 특정 Travel + channelId로 조회
    Optional<TravelChannel> findByTravelIdAndChannelIdAndIsDeletedFalse(String travelId, String channelId);

    // 중복 체크
    boolean existsByTravelIdAndChannelIdAndIsDeletedFalse(String travelId, String channelId);

    // Travel 삭제 시 일괄 정리
    void deleteByTravelId(String travelId);
}
