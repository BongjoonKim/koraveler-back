package server.nadeliv.discovery.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.discovery.model.entities.DestinationDigest;

import java.util.Optional;

@Repository
public interface DestinationDigestRepo extends MongoRepository<DestinationDigest, String> {

    Optional<DestinationDigest> findByDestinationKeyAndLocale(String destinationKey, String locale);
}
