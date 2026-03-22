package server.koraveler.travel.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.koraveler.travel.model.entities.TravelChannel;
import server.koraveler.travel.model.enums.ChannelContextType;

import java.util.List;
import java.util.Optional;

public interface TravelChannelRepo extends MongoRepository<TravelChannel, String> {

    // 특정 Travel의 채널 목록 (삭제되지 않은 것만, 정렬 순서)
    List<TravelChannel> findByTravelIdAndDeletedFalseOrderByDisplayOrderAsc(String travelId);

    // 특정 Travel + contextType으로 필터
    List<TravelChannel> findByTravelIdAndContextTypeAndDeletedFalse(
            String travelId, ChannelContextType contextType);

    // 특정 channelId로 조회 (역방향 — 채널에서 Travel 찾기)
    Optional<TravelChannel> findByChannelIdAndDeletedFalse(String channelId);

    // 특정 Travel + channelId로 조회
    Optional<TravelChannel> findByTravelIdAndChannelIdAndDeletedFalse(String travelId, String channelId);

    // 중복 체크
    boolean existsByTravelIdAndChannelIdAndDeletedFalse(String travelId, String channelId);

    // Travel 삭제 시 일괄 정리
    void deleteByTravelId(String travelId);
}
