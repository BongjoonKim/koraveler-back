package server.nadeliv.i18n.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.i18n.model.entities.PostTranslation;
import server.nadeliv.i18n.model.enums.TranslationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostTranslationRepo extends MongoRepository<PostTranslation, String> {

    Optional<PostTranslation> findByPostIdAndLocale(String postId, String locale);

    List<PostTranslation> findByPostId(String postId);

    List<PostTranslation> findByPostIdAndStatus(String postId, TranslationStatus status);

    boolean existsByPostIdAndLocale(String postId, String locale);

    // sitemap.xml 용: 번역 완료된 모든 문서 조회
    List<PostTranslation> findByStatus(TranslationStatus status);

    void deleteByPostId(String postId);

    // 여러 문서의 번역을 한번에 조회 (블로그 목록용)
    List<PostTranslation> findByPostIdInAndLocaleAndStatusIn(
            List<String> postIds, String locale, List<TranslationStatus> statuses);
}
