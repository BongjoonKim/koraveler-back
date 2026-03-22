package server.nadeliv.place.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.place.model.entities.PlaceSearchHistory;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceSearchHistoryRepo extends MongoRepository<PlaceSearchHistory, String> {

    // 사용자별 검색 이력 조회
    List<PlaceSearchHistory> findByUserIdOrderByLastSearchedAtDesc(String userId);

    // 특정 키워드로 검색한 이력 조회
    List<PlaceSearchHistory> findByUserIdAndKeyword(String userId, String keyword);

    // 특정 장소 검색 이력 조회
    Optional<PlaceSearchHistory> findByUserIdAndPlaceId(String userId, String placeId);
}