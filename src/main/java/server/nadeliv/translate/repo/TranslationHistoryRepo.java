package server.nadeliv.translate.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.nadeliv.translate.model.entities.TranslationHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationHistoryRepo extends MongoRepository<TranslationHistory, String> {
    // 특정 사용자의 번역 이력 조회 (최신순)
    Page<TranslationHistory> findByUserIdOrderByCreatedDesc(String userId, Pageable pageable);

    // 특정 사용자의 좋아요한 번역만 조회
    Page<TranslationHistory> findByUserIdAndIsLikedTrueOrderByCreatedDesc(String userId, Pageable pageable);

    // 특정 사용자의 모든 번역 이력 조회 (최신순, 페이징 없이)
    List<TranslationHistory> findByUserIdOrderByCreatedDesc(String userId);

    // 사용자와 ID로 특정 번역 조회 (권한 체크용)
    Optional<TranslationHistory> findByIdAndUserId(String id, String userId);

    // 특정 언어 쌍의 번역 이력 조회
    Page<TranslationHistory> findByUserIdAndSourceLanguageAndTargetLanguageOrderByCreatedDateDesc(
            String userId, String sourceLanguage, String targetLanguage, Pageable pageable);

    // 텍스트 검색 (source 또는 target에서)
    @Query("{'userId': ?0, '$or': [{'sourceText': {$regex: ?1, $options: 'i'}}, {'targetText': {$regex: ?1, $options: 'i'}}]}")
    Page<TranslationHistory> searchByText(String userId, String searchText, Pageable pageable);

    // 특정 기간 내 번역 이력 조회
    Page<TranslationHistory> findByUserIdAndCreatedDateBetweenOrderByCreatedDateDesc(
            String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // 사용자의 번역 통계용 - 총 번역 수
    long countByUserId(String userId);

    // 사용자의 좋아요한 번역 수
    long countByUserIdAndIsLikedTrue(String userId);

    // 중복 번역 체크 (같은 텍스트를 최근에 번역했는지)
    @Query("{'userId': ?0, 'sourceText': ?1, 'sourceLanguage': ?2, 'targetLanguage': ?3, 'createdDate': {$gte: ?4}}")
    Optional<TranslationHistory> findRecentDuplicateTranslation(
            String userId, String sourceText, String sourceLanguage,
            String targetLanguage, LocalDateTime since);

    // 자주 사용하는 번역 찾기 (동일한 sourceText가 여러 번 번역된 경우)
    @Query(value = "{'userId': ?0}",
            fields = "{'sourceText': 1, 'targetText': 1, 'sourceLanguage': 1, 'targetLanguage': 1}")
    List<TranslationHistory> findFrequentTranslations(String userId, Pageable pageable);

    // 만료 예정 번역들 조회 (알림용)
    @Query("{'userId': ?0, 'isLiked': false, 'expireAt': {$lte: ?1, $gte: ?2}}")
    List<TranslationHistory> findExpiringTranslations(String userId, LocalDateTime before, LocalDateTime after);

    // 사용자의 모든 번역 이력 삭제 (계정 삭제 시)
    void deleteAllByUserId(String userId);

    // 특정 ID 리스트의 번역들을 좋아요 처리
    @Query("{'_id': {$in: ?0}, 'userId': ?1}")
    List<TranslationHistory> findAllByIdsAndUserId(List<String> ids, String userId);
}
