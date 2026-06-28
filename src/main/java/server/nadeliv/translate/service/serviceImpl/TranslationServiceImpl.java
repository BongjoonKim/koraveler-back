package server.nadeliv.translate.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.nadeliv.translate.dto.*;
import server.nadeliv.translate.model.entities.TranslationHistory;
import server.nadeliv.translate.model.enums.ExportFormat;
import server.nadeliv.translate.repo.TranslationHistoryCustomRepo;
import server.nadeliv.translate.repo.TranslationHistoryRepo;
import server.nadeliv.translate.service.TranslationService;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TranslationServiceImpl implements TranslationService {

    private final TranslationHistoryRepo translationHistoryRepo;
    private final TranslationHistoryCustomRepo translationHistoryCustomRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cloud.aws.region.static}")
    private String awsRegion;

    @Value("${cloud.aws.bedrock.model-id}")
    private String bedrockModelId;

    @Override
    public TranslationResponse translate(String userId, TranslationRequest request) {
        log.info("Translating text for user: {}, from {} to {}", userId,
                request.getSourceLanguage(), request.getTargetLanguage());

        TranslationResponse response;

        // 중복 체크 - 최근 동일 번역이 있으면 재사용
        if (Boolean.TRUE.equals(request.getUseCache())) {
            TranslationHistory duplicate = checkDuplicateTranslation(
                    userId,
                    request.getSourceText(),
                    request.getSourceLanguage(),
                    request.getTargetLanguage()
            );

            if (duplicate != null) {
                log.info("Found existing translation for user: {}", userId);
                response = TranslationResponse.builder()
                        .id(duplicate.getId())
                        .sourceText(duplicate.getSourceText())
                        .targetText(duplicate.getTargetText())
                        .sourceLanguage(duplicate.getSourceLanguage())
                        .targetLanguage(duplicate.getTargetLanguage())
                        .translatedAt(duplicate.getCreated())
                        .fromCache(true)
                        .isLiked(duplicate.isLiked())
                        .pronunciation(duplicate.getPronunciation())
                        .build();
                return response;
            }
        }

        // AWS Bedrock API 호출하여 번역 및 발음 받기
        TranslationWithPronunciation result = callBedrockApiWithPronunciation(request);

        // 번역 이력 저장
        TranslationHistory history = TranslationHistory.builder()
                .userId(userId)
                .sourceText(request.getSourceText())
                .targetText(result.getTranslation())
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .pronunciation(result.getPronunciation())
                .isLiked(false)
                .expireAt(LocalDateTime.now().plusDays(30))
                .build();
        history.setCreated(LocalDateTime.now());
        history.setUpdated(LocalDateTime.now());

        TranslationHistory savedHistory = translationHistoryRepo.save(history);

        response = TranslationResponse.builder()
                .id(savedHistory.getId())
                .sourceText(savedHistory.getSourceText())
                .targetText(savedHistory.getTargetText())
                .sourceLanguage(savedHistory.getSourceLanguage())
                .targetLanguage(savedHistory.getTargetLanguage())
                .translatedAt(savedHistory.getCreated())
                .fromCache(false)
                .isLiked(savedHistory.isLiked())
                .pronunciation(savedHistory.getPronunciation())
                .build();

        return response;
    }

    private TranslationWithPronunciation callBedrockApiWithPronunciation(TranslationRequest request) {
        try {
            BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            String systemPrompt = String.format(
                    "You are a professional translator. Translate the following text from %s to %s. " +
                            "Additionally, provide a romanized pronunciation guide (using Latin alphabet) for the translated text. " +
                            "Respond ONLY in this JSON format without any markdown or code blocks:\n" +
                            "{\"translation\": \"translated text here\", \"pronunciation\": \"romanized pronunciation here\"}\n" +
                            "Context: %s",
                    getLanguageName(request.getSourceLanguage()),
                    getLanguageName(request.getTargetLanguage()),
                    request.getContext() != null ? request.getContext() : "general"
            );

            ConverseRequest converseRequest = ConverseRequest.builder()
                    .modelId(bedrockModelId)
                    .system(SystemContentBlock.builder().text(systemPrompt).build())
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(request.getSourceText()))
                            .build())
                    .build();

            ConverseResponse converseResponse = bedrockClient.converse(converseRequest);

            String content = converseResponse.output().message().content().get(0).text().trim();

            try {
                Map<String, String> translationResult = objectMapper.readValue(content, Map.class);
                return new TranslationWithPronunciation(
                        translationResult.get("translation"),
                        translationResult.get("pronunciation")
                );
            } catch (Exception e) {
                log.warn("Failed to parse JSON response, using fallback", e);
                return new TranslationWithPronunciation(content, null);
            }

        } catch (Exception e) {
            log.error("Error calling AWS Bedrock API: ", e);
            return new TranslationWithPronunciation(
                    "Translation failed: " + e.getMessage(),
                    null
            );
        }
    }

    private String getLanguageName(String code) {
        Map<String, String> languages = Map.ofEntries(
                Map.entry("ko", "Korean"),
                Map.entry("en", "English"),
                Map.entry("ja", "Japanese"),
                Map.entry("zh", "Chinese"),
                Map.entry("es", "Spanish"),
                Map.entry("fr", "French"),
                Map.entry("de", "German"),
                Map.entry("it", "Italian"),
                Map.entry("pt", "Portuguese"),
                Map.entry("ru", "Russian"),
                Map.entry("ar", "Arabic"),
                Map.entry("hi", "Hindi")
        );
        return languages.getOrDefault(code, code);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslationHistory> getTranslationHistory(String userId, Pageable pageable) {
        return translationHistoryRepo.findByUserIdOrderByCreatedDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslationHistory> getLikedTranslations(String userId, Pageable pageable) {
        return translationHistoryRepo.findByUserIdAndIsLikedTrueOrderByCreatedDesc(userId, pageable);
    }

    @Override
    public TranslationHistory toggleLike(String userId, String translationId) {
        TranslationHistory translation = translationHistoryRepo.findByIdAndUserId(translationId, userId)
                .orElseThrow(() -> new RuntimeException("Translation not found"));

        if (translation.isLiked()) {
            translation.setLiked(false);
            translation.setExpireAt(LocalDateTime.now().plusDays(30));
        } else {
            translation.markAsLiked();
        }

        translation.setUpdated(LocalDateTime.now());
        return translationHistoryRepo.save(translation);
    }

    @Override
    public void deleteTranslation(String userId, String translationId) {
        TranslationHistory translation = translationHistoryRepo.findByIdAndUserId(translationId, userId)
                .orElseThrow(() -> new RuntimeException("Translation not found"));
        translationHistoryRepo.delete(translation);
    }

    @Override
    public void deleteTranslations(String userId, List<String> translationIds) {
        List<TranslationHistory> translations = translationHistoryRepo.findAllByIdsAndUserId(translationIds, userId);
        translationHistoryRepo.deleteAll(translations);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslationHistory> searchTranslations(String userId, String searchText, Pageable pageable) {
        return translationHistoryRepo.searchByText(userId, searchText, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslationHistory> getTranslationsByLanguages(String userId, String sourceLanguage,
                                                               String targetLanguage, Pageable pageable) {
        return translationHistoryRepo.findByUserIdAndSourceLanguageAndTargetLanguageOrderByCreatedDateDesc(
                userId, sourceLanguage, targetLanguage, pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranslationHistory> getTranslationsByDateRange(String userId, LocalDateTime startDate,
                                                               LocalDateTime endDate, Pageable pageable) {
        return translationHistoryRepo.findByUserIdAndCreatedDateBetweenOrderByCreatedDateDesc(
                userId, startDate, endDate, pageable
        );
    }

    private String categorizeText(String text) {
        // 간단한 카테고리 분류 로직
        if (text.toLowerCase().contains("hello") || text.toLowerCase().contains("안녕")) {
            return "인사";
        } else if (text.toLowerCase().contains("thank") || text.toLowerCase().contains("감사")) {
            return "감사";
        } else if (text.toLowerCase().contains("where") || text.toLowerCase().contains("어디")) {
            return "길찾기";
        } else if (text.toLowerCase().contains("how much") || text.toLowerCase().contains("얼마")) {
            return "쇼핑";
        } else if (text.toLowerCase().contains("delicious") || text.toLowerCase().contains("맛있")) {
            return "음식";
        } else {
            return "일반";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LanguageStatistics> getLanguageStatistics(String userId) {
        return translationHistoryCustomRepo.getLanguageStatistics(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TranslationStatistics getTranslationStatistics(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return translationHistoryCustomRepo.getTranslationStatistics(userId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDateTime, Long> getDailyTranslationCount(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return translationHistoryCustomRepo.getDailyTranslationCount(userId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTranslations(String userId, ExportFormat format, boolean onlyLiked) {
        List<TranslationHistory> translations;

        if (onlyLiked) {
            translations = translationHistoryCustomRepo.exportLikedTranslations(userId);
        } else {
            translations = translationHistoryRepo.findByUserIdOrderByCreatedDesc(userId);
        }

        try {
            switch (format) {
                case CSV:
                    return exportToCsv(translations);
                case JSON:
                    return exportToJson(translations);
                case TXT:
                    return exportToTxt(translations);
                default:
                    throw new RuntimeException("Unsupported export format");
            }
        } catch (Exception e) {
            log.error("Error exporting translations: ", e);
            throw new RuntimeException("Failed to export translations", e);
        }
    }

    private byte[] exportToCsv(List<TranslationHistory> translations) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // Header
        writer.println("Date,Source Language,Target Language,Source Text,Target Text,Liked");

        // Data
        for (TranslationHistory translation : translations) {
            writer.println(String.format("%s,%s,%s,\"%s\",\"%s\",%s",
                    translation.getCreated().toString(),
                    translation.getSourceLanguage(),
                    translation.getTargetLanguage(),
                    translation.getSourceText().replace("\"", "\"\""),  // Escape quotes
                    translation.getTargetText().replace("\"", "\"\""),  // Escape quotes
                    String.valueOf(translation.isLiked())
            ));
        }

        writer.flush();
        writer.close();
        return baos.toByteArray();
    }

    private byte[] exportToJson(List<TranslationHistory> translations) throws Exception {
        return objectMapper.writeValueAsBytes(translations);
    }

    private byte[] exportToTxt(List<TranslationHistory> translations) {
        StringBuilder sb = new StringBuilder();
        for (TranslationHistory translation : translations) {
            sb.append("=".repeat(50)).append("\n");
            sb.append("Date: ").append(translation.getCreated()).append("\n");
            sb.append("Source (").append(translation.getSourceLanguage()).append("): ")
                    .append(translation.getSourceText()).append("\n");
            sb.append("Target (").append(translation.getTargetLanguage()).append("): ")
                    .append(translation.getTargetText()).append("\n");
            sb.append("Liked: ").append(translation.isLiked()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<TranslationHistory> importTranslations(String userId, List<TranslationHistory> translations) {
        // 사용자 ID 설정 및 유효성 검증
        translations.forEach(t -> {
            t.setUserId(userId);
            t.setCreated(LocalDateTime.now());
            t.setUpdated(LocalDateTime.now());
            if (!t.isLiked()) {
                t.setExpireAt(LocalDateTime.now().plusDays(30));
            }
        });

        return translationHistoryCustomRepo.bulkSaveTranslations(translations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslationHistory> findSimilarTranslations(String userId, String text, double minSimilarity) {
        return translationHistoryCustomRepo.findSimilarTranslations(userId, text, minSimilarity);
    }

    @Override
    public void deleteAllTranslations(String userId) {
        translationHistoryRepo.deleteAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TranslationHistory checkDuplicateTranslation(String userId, String sourceText,
                                                        String sourceLanguage, String targetLanguage) {
        // 최근 24시간 내 동일한 번역 확인
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return translationHistoryRepo.findRecentDuplicateTranslation(
                userId, sourceText, sourceLanguage, targetLanguage, since
        ).orElse(null);
    }

    // 내부 헬퍼 클래스
    private static class TranslationWithPronunciation {
        private final String translation;
        private final String pronunciation;

        public TranslationWithPronunciation(String translation, String pronunciation) {
            this.translation = translation;
            this.pronunciation = pronunciation;
        }

        public String getTranslation() {
            return translation;
        }

        public String getPronunciation() {
            return pronunciation;
        }
    }
}