package server.nadeliv.i18n.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.i18n.dto.*;
import server.nadeliv.i18n.service.I18nTranslationService;
import server.nadeliv.i18n.service.I18nLocaleDetector;
import server.nadeliv.users.dto.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/v1/i18n")
@RequiredArgsConstructor
@Slf4j
public class I18nController {

    private final I18nTranslationService i18nTranslationService;
    private final I18nLocaleDetector localeDetector;

    // ─── 독자용 API (비인증 허용) ───

    /**
     * 특정 글의 번역본 조회.
     * locale 파라미터 없으면 Accept-Language / 쿠키 / 원본 fallback.
     */
    @GetMapping("/ps/posts/{postId}")
    public ResponseEntity<?> getTranslatedPost(
            @PathVariable String postId,
            @RequestParam(required = false) String locale,
            HttpServletRequest request
    ) {
        try {
            String resolvedLocale = localeDetector.resolve(request, locale);
            TranslatedPostDTO result = i18nTranslationService.getTranslatedPost(postId, resolvedLocale);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("번역 글 조회 실패: postId={}", postId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Post not found: " + postId);
        }
    }

    /**
     * 지원 언어 목록 조회.
     */
    @GetMapping("/ps/locales")
    public ResponseEntity<List<AvailableLocaleDTO>> getSupportedLocales() {
        return ResponseEntity.ok(i18nTranslationService.getSupportedLocales());
    }

    // ─── 작성자용 API (인증 필요) ───

    /**
     * 전체 번역 상태 조회 (대시보드용).
     */
    @GetMapping("/posts/{postId}/translations")
    public ResponseEntity<?> getTranslationStatus(
            @PathVariable String postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            TranslationStatusOverviewDTO result = i18nTranslationService.getTranslationStatus(postId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("번역 상태 조회 실패: postId={}", postId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Post not found: " + postId);
        }
    }

    /**
     * 특정 번역본 상세 조회 (편집용).
     */
    @GetMapping("/posts/{postId}/translations/{locale}")
    public ResponseEntity<?> getTranslationDetail(
            @PathVariable String postId,
            @PathVariable String locale,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            TranslationDetailDTO result = i18nTranslationService.getTranslationDetail(postId, locale);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("번역 상세 조회 실패: postId={}, locale={}", postId, locale, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Translation not found");
        }
    }

    /**
     * 번역본 수동 수정.
     */
    @PutMapping("/posts/{postId}/translations/{locale}")
    public ResponseEntity<?> updateTranslation(
            @PathVariable String postId,
            @PathVariable String locale,
            @RequestBody TranslationEditRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            TranslationDetailDTO result = i18nTranslationService
                    .updateTranslation(postId, locale, request, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("번역 수정 실패: postId={}, locale={}", postId, locale, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Translation update failed: " + e.getMessage());
        }
    }

    /**
     * 특정 언어 재번역 요청.
     * 수동 편집된 번역이면 confirmed=true 필요.
     */
    @PostMapping("/posts/{postId}/translations/{locale}/retranslate")
    public ResponseEntity<?> retranslate(
            @PathVariable String postId,
            @PathVariable String locale,
            @RequestBody(required = false) RetranslateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[I18n] 재번역 API 호출: postId={}, locale={}, userId={}", postId, locale, userDetails.getUsername());
        try {
            boolean confirmed = request != null && request.isConfirmed();
            RetranslateResponseDTO result = i18nTranslationService.retranslate(postId, locale, confirmed);

            if (result.isRequiresConfirmation()) {
                log.info("[I18n] 수동 편집 확인 필요 응답: postId={}, locale={}", postId, locale);
                return ResponseEntity.ok(result);
            }
            log.info("[I18n] 재번역 큐잉 성공 응답(202): postId={}, locale={}", postId, locale);
            return ResponseEntity.accepted().body(result);
        } catch (Exception e) {
            log.error("[I18n] 재번역 요청 실패: postId={}, locale={}", postId, locale, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Retranslate failed: " + e.getMessage());
        }
    }

    /**
     * 전체 재번역 요청.
     */
    @PostMapping("/posts/{postId}/translations/retranslate-all")
    public ResponseEntity<?> retranslateAll(
            @PathVariable String postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[I18n] 전체 재번역 API 호출: postId={}, userId={}", postId, userDetails.getUsername());
        try {
            i18nTranslationService.retranslateAll(postId);
            log.info("[I18n] 전체 재번역 큐잉 성공 응답(202): postId={}", postId);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("[I18n] 전체 재번역 요청 실패: postId={}", postId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Retranslate all failed: " + e.getMessage());
        }
    }

    // ─── 관리자용 API (인증 필요) ───

    /**
     * 기존 글 일괄 번역 큐잉.
     * 이미 번역된 글은 건너뛰고, 미번역 글만 큐에 추가.
     */
    @PostMapping("/admin/translate-all-existing")
    public ResponseEntity<?> translateAllExisting(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            int queuedCount = i18nTranslationService.queueAllExistingPosts();
            return ResponseEntity.accepted()
                    .body(java.util.Map.of(
                            "message", "Batch translation queued",
                            "queuedPosts", queuedCount
                    ));
        } catch (Exception e) {
            log.error("기존 글 일괄 번역 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Batch translation failed: " + e.getMessage());
        }
    }

    /**
     * 깨진 번역(잘린 모델 응답이 그대로 저장된 건) 탐지 후 재번역 큐잉.
     */
    @PostMapping("/admin/repair-broken-translations")
    public ResponseEntity<?> repairBrokenTranslations(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            int queuedCount = i18nTranslationService.repairBrokenTranslations();
            return ResponseEntity.accepted()
                    .body(java.util.Map.of(
                            "message", "Broken translations re-queued",
                            "queuedTranslations", queuedCount
                    ));
        } catch (Exception e) {
            log.error("깨진 번역 복구 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Repair failed: " + e.getMessage());
        }
    }
}
