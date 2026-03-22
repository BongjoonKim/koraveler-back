package server.nadeliv.translate.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.nadeliv.translate.dto.*;
import server.nadeliv.translate.model.entities.TranslationHistory;
import server.nadeliv.translate.model.enums.ExportFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TranslationService {

    /**
     * 텍스트 번역
     * @param userId 사용자 ID
     * @param request 번역 요청 정보
     * @return 번역 결과
     */
    TranslationResponse translate(String userId, TranslationRequest request);

    /**
     * 번역 이력 조회 (페이징)
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 번역 이력 페이지
     */
    Page<TranslationHistory> getTranslationHistory(String userId, Pageable pageable);

    /**
     * 좋아요한 번역 이력 조회
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요한 번역 이력 페이지
     */
    Page<TranslationHistory> getLikedTranslations(String userId, Pageable pageable);

    /**
     * 번역 좋아요 토글
     * @param userId 사용자 ID
     * @param translationId 번역 ID
     * @return 업데이트된 번역 이력
     */
    TranslationHistory toggleLike(String userId, String translationId);

    /**
     * 번역 이력 삭제
     * @param userId 사용자 ID
     * @param translationId 번역 ID
     */
    void deleteTranslation(String userId, String translationId);

    /**
     * 번역 이력 일괄 삭제
     * @param userId 사용자 ID
     * @param translationIds 번역 ID 리스트
     */
    void deleteTranslations(String userId, List<String> translationIds);

    /**
     * 텍스트로 번역 이력 검색
     * @param userId 사용자 ID
     * @param searchText 검색어
     * @param pageable 페이징 정보
     * @return 검색된 번역 이력
     */
    Page<TranslationHistory> searchTranslations(String userId, String searchText, Pageable pageable);

    /**
     * 특정 언어 쌍의 번역 이력 조회
     * @param userId 사용자 ID
     * @param sourceLanguage 소스 언어
     * @param targetLanguage 타겟 언어
     * @param pageable 페이징 정보
     * @return 언어별 번역 이력
     */
    Page<TranslationHistory> getTranslationsByLanguages(String userId, String sourceLanguage,
                                                        String targetLanguage, Pageable pageable);

    /**
     * 기간별 번역 이력 조회
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 기간별 번역 이력
     */
    Page<TranslationHistory> getTranslationsByDateRange(String userId, LocalDateTime startDate,
                                                        LocalDateTime endDate, Pageable pageable);

    /**
     * 언어별 통계 조회
     * @param userId 사용자 ID
     * @return 언어별 번역 통계
     */
    List<LanguageStatistics> getLanguageStatistics(String userId);

    /**
     * 번역 통계 조회
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 번역 통계
     */
    TranslationStatistics getTranslationStatistics(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 일별 번역 횟수 통계
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 일별 번역 횟수 맵
     */
    Map<LocalDateTime, Long> getDailyTranslationCount(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 번역 이력 내보내기
     * @param userId 사용자 ID
     * @param format 내보내기 형식
     * @param onlyLiked 좋아요한 것만 내보내기 여부
     * @return 내보낸 데이터 (바이트 배열)
     */
    byte[] exportTranslations(String userId, ExportFormat format, boolean onlyLiked);

    /**
     * 번역 이력 가져오기 (벌크)
     * @param userId 사용자 ID
     * @param translations 번역 이력 리스트
     * @return 저장된 번역 이력
     */
    List<TranslationHistory> importTranslations(String userId, List<TranslationHistory> translations);

    /**
     * 유사한 번역 찾기
     * @param userId 사용자 ID
     * @param text 검색할 텍스트
     * @param minSimilarity 최소 유사도 (0.0 ~ 1.0)
     * @return 유사한 번역 리스트
     */
    List<TranslationHistory> findSimilarTranslations(String userId, String text, double minSimilarity);

    /**
     * 사용자의 모든 번역 이력 삭제
     * @param userId 사용자 ID
     */
    void deleteAllTranslations(String userId);

    /**
     * 중복 번역 체크
     * @param userId 사용자 ID
     * @param sourceText 원문
     * @param sourceLanguage 소스 언어
     * @param targetLanguage 타겟 언어
     * @return 중복 번역 존재 시 해당 번역, 없으면 null
     */
    TranslationHistory checkDuplicateTranslation(String userId, String sourceText,
                                                 String sourceLanguage, String targetLanguage);
}