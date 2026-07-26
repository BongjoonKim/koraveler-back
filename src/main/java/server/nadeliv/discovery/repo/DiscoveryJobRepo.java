package server.nadeliv.discovery.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.discovery.model.entities.DiscoveryJob;
import server.nadeliv.discovery.model.enums.DiscoveryJobStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveryJobRepo extends MongoRepository<DiscoveryJob, String> {

    List<DiscoveryJob> findTop3ByStatusOrderByCreatedAtAsc(DiscoveryJobStatus status);

    Optional<DiscoveryJob> findFirstByDestinationKeyAndLocaleAndStatusIn(
            String destinationKey, String locale, List<DiscoveryJobStatus> statuses);

    Optional<DiscoveryJob> findFirstByDestinationKeyAndLocaleOrderByCreatedAtDesc(
            String destinationKey, String locale);
}
