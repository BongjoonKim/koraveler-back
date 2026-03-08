package server.koraveler.i18n.service;

import server.koraveler.blog.model.Documents;
import server.koraveler.i18n.dto.*;
import server.koraveler.i18n.model.entities.TranslationJob;

import java.util.List;

public interface I18nTranslationService {

    // 번역 큐잉: 글 저장/수정 시 호출
    void queueTranslations(Documents post);

    // 번역 작업 처리: Worker에서 호출
    void processJob(TranslationJob job);

    // 독자용: locale에 맞는 번역된 글 조회
    TranslatedPostDTO getTranslatedPost(String postId, String locale);

    // 독자용: 지원 언어 목록
    List<AvailableLocaleDTO> getSupportedLocales();

    // 작성자용: 전체 번역 상태 조회
    TranslationStatusOverviewDTO getTranslationStatus(String postId);

    // 작성자용: 특정 번역본 상세 조회 (편집용)
    TranslationDetailDTO getTranslationDetail(String postId, String locale);

    // 작성자용: 번역본 수동 수정
    TranslationDetailDTO updateTranslation(String postId, String locale,
                                           TranslationEditRequestDTO request, String userId);

    // 작성자용: 특정 언어 재번역 요청
    RetranslateResponseDTO retranslate(String postId, String locale, boolean confirmed);

    // 작성자용: 전체 재번역 요청
    void retranslateAll(String postId);

    // 관리자용: 기존 글 일괄 번역 큐잉
    int queueAllExistingPosts();
}
