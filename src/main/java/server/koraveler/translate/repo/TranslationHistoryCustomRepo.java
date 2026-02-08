package server.koraveler.translate.repo;

import server.koraveler.translate.dto.LanguageStatistics;
import server.koraveler.translate.dto.TranslationStatistics;
import server.koraveler.translate.model.entities.TranslationHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TranslationHistoryCustomRepo {

    // 언어별 번역 통계
    List<LanguageStatistics> getLanguageStatistics(String userId);

    // 기간별 번역 통계
    TranslationStatistics getTranslationStatistics(String userId, LocalDateTime startDate, LocalDateTime endDate);

    // 자주 번역하는 텍스트 Top N
    List<Map<String, Object>> getFrequentlyTranslatedTexts(String userId, int limit);

    // 일별 번역 횟수 통계
    Map<LocalDateTime, Long> getDailyTranslationCount(String userId, LocalDateTime startDate, LocalDateTime endDate);

    // 배치로 여러 번역 저장 (벌크 작업)
    List<TranslationHistory> bulkSaveTranslations(List<TranslationHistory> translations);

    // 좋아요한 번역들만 내보내기용 조회
    List<TranslationHistory> exportLikedTranslations(String userId);

    // 비슷한 번역 찾기 (유사도 검색)
    List<TranslationHistory> findSimilarTranslations(String userId, String text, double minSimilarity);
}
