package server.koraveler.travel.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.travel.model.entities.TravelMedia;

import java.util.List;

@Repository
public interface TravelMediaRepo extends MongoRepository<TravelMedia, String> {

    Page<TravelMedia> findByTravelIdOrderByCreatedDesc(String travelId, Pageable pageable);

    List<TravelMedia> findByTravelId(String travelId);

    void deleteByTravelId(String travelId);

    Long countByTravelId(String travelId);
}
