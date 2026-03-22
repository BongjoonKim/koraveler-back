package server.nadeliv.travel.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.travel.model.entities.TravelUsers;
import server.nadeliv.travel.model.enums.TravelRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelUsersRepo extends MongoRepository<TravelUsers, String> {

    List<TravelUsers> findByTravelId(String travelId);

    List<TravelUsers> findByUserId(String userId);

    Optional<TravelUsers> findByTravelIdAndUserId(String travelId, String userId);

    boolean existsByTravelIdAndUserId(String travelId, String userId);

    void deleteByTravelIdAndUserId(String travelId, String userId);

    void deleteByTravelId(String travelId);

    Long countByTravelIdAndRole(String travelId, TravelRole role);

    Long countByTravelId(String travelId);
}
