package server.koraveler.translate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import server.koraveler.translate.dto.*;
import server.koraveler.translate.model.entities.TranslationHistory;
import server.koraveler.translate.model.enums.ExportFormat;
import server.koraveler.translate.service.TranslationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/translate")
@RequiredArgsConstructor
@Slf4j
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    // 텍스트 번역
    @PostMapping("")
    public ResponseEntity<?> translate(
            @RequestBody TranslationRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            TranslationResponse response = translationService.translate(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Translation error: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 번역 이력 조회
    @GetMapping("/history")
    public Page<TranslationHistory> getTranslationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            return translationService.getTranslationHistory(userId, PageRequest.of(page, size));
        } catch (Exception e) {
            log.error("Error retrieving history: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 좋아요한 번역 조회
    @GetMapping("/liked")
    public Page<TranslationHistory> getLikedTranslations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            return translationService.getLikedTranslations(userId, PageRequest.of(page, size));
        } catch (Exception e) {
            log.error("Error retrieving liked translations: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 번역 좋아요 토글
    @PutMapping("/like")
    public ResponseEntity<?> toggleLike(
            @RequestParam("id") String translationId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            TranslationHistory updated = translationService.toggleLike(userId, translationId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error toggling like: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 번역 삭제
    @DeleteMapping("")
    public ResponseEntity<?> deleteTranslation(
            @RequestParam("id") String translationId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            translationService.deleteTranslation(userId, translationId);
            Map<String, String> response = new HashMap<>();
            response.put("id", translationId);
            response.put("message", "Translation deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting translation: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 번역 일괄 삭제
    @DeleteMapping("/bulk")
    public ResponseEntity<?> deleteTranslations(
            @RequestBody List<String> translationIds,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            translationService.deleteTranslations(userId, translationIds);
            Map<String, Object> response = new HashMap<>();
            response.put("count", translationIds.size());
            response.put("message", "Translations deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting translations: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 번역 검색
    @GetMapping("/search")
    public Page<TranslationHistory> searchTranslations(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            return translationService.searchTranslations(userId, keyword, PageRequest.of(page, size));
        } catch (Exception e) {
            log.error("Error searching translations: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 언어별 번역 조회
    @GetMapping("/by-language")
    public Page<TranslationHistory> getTranslationsByLanguages(
            @RequestParam("source") String sourceLanguage,
            @RequestParam("target") String targetLanguage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            return translationService.getTranslationsByLanguages(
                    userId, sourceLanguage, targetLanguage, PageRequest.of(page, size));
        } catch (Exception e) {
            log.error("Error retrieving translations by language: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 자주 사용하는 구문 조회
//    @GetMapping("/quick-phrases")
//    public List<QuickPhrase> getQuickPhrases(
//            @RequestParam(defaultValue = "10") int limit,
//            Authentication authentication) {
//        try {
//            String userId = authentication.getName();
//            return translationService.getQuickPhrases(userId, limit);
//        } catch (Exception e) {
//            log.error("Error retrieving quick phrases: ", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }

    // 언어별 통계
    @GetMapping("/statistics/languages")
    public List<LanguageStatistics> getLanguageStatistics(Authentication authentication) {
        try {
            String userId = authentication.getName();
            return translationService.getLanguageStatistics(userId);
        } catch (Exception e) {
            log.error("Error retrieving language statistics: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 번역 통계
    @GetMapping("/statistics")
    public TranslationStatistics getTranslationStatistics(
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr);
            return translationService.getTranslationStatistics(userId, startDate, endDate);
        } catch (Exception e) {
            log.error("Error retrieving statistics: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 일별 번역 횟수
    @GetMapping("/statistics/daily")
    public Map<LocalDateTime, Long> getDailyTranslationCount(
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr);
            return translationService.getDailyTranslationCount(userId, startDate, endDate);
        } catch (Exception e) {
            log.error("Error retrieving daily count: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 번역 내보내기
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTranslations(
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam(defaultValue = "false") boolean onlyLiked,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            ExportFormat exportFormat = ExportFormat.valueOf(format.toUpperCase());
            byte[] data = translationService.exportTranslations(userId, exportFormat, onlyLiked);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(exportFormat.getMimeType()));
            headers.setContentDispositionFormData("attachment",
                    "translations_" + System.currentTimeMillis() + "." + exportFormat.getExtension());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (Exception e) {
            log.error("Error exporting translations: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 유사한 번역 찾기
    @GetMapping("/similar")
    public List<TranslationHistory> findSimilarTranslations(
            @RequestParam("text") String text,
            @RequestParam(defaultValue = "0.5") double minSimilarity,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            return translationService.findSimilarTranslations(userId, text, minSimilarity);
        } catch (Exception e) {
            log.error("Error finding similar translations: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // 모든 번역 삭제
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllTranslations(Authentication authentication) {
        try {
            String userId = authentication.getName();
            translationService.deleteAllTranslations(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "All translations deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting all translations: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}