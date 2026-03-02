package server.koraveler.i18n.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.i18n.model.entities.PostTranslation;
import server.koraveler.i18n.model.enums.TranslationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostTranslationRepo extends MongoRepository<PostTranslation, String> {

    Optional<PostTranslation> findByPostIdAndLocale(String postId, String locale);

    List<PostTranslation> findByPostId(String postId);

    List<PostTranslation> findByPostIdAndStatus(String postId, TranslationStatus status);

    boolean existsByPostIdAndLocale(String postId, String locale);

    void deleteByPostId(String postId);
}
