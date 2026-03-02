package server.koraveler.i18n.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.i18n.model.entities.TranslationJob;
import server.koraveler.i18n.model.enums.JobStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationJobRepo extends MongoRepository<TranslationJob, String> {

    List<TranslationJob> findTop10ByStatusOrderByCreatedAtAsc(JobStatus status);

    Optional<TranslationJob> findByPostIdAndTargetLocaleAndStatusIn(
            String postId, String targetLocale, List<JobStatus> statuses);

    List<TranslationJob> findByPostId(String postId);

    void deleteByPostId(String postId);
}
