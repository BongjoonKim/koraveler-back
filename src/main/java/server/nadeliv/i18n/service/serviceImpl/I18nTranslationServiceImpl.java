package server.nadeliv.i18n.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.nadeliv.blog.model.Documents;
import server.nadeliv.blog.repo.BlogsRepo;
import server.nadeliv.i18n.dto.*;
import server.nadeliv.i18n.model.entities.PostTranslation;
import server.nadeliv.i18n.model.entities.TranslationJob;
import server.nadeliv.i18n.model.enums.JobStatus;
import server.nadeliv.i18n.model.enums.TranslatedBy;
import server.nadeliv.i18n.model.enums.TranslationStatus;
import server.nadeliv.i18n.repo.PostTranslationRepo;
import server.nadeliv.i18n.repo.TranslationJobRepo;
import server.nadeliv.i18n.service.BedrockPostTranslationClient;
import server.nadeliv.i18n.service.I18nTranslationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class I18nTranslationServiceImpl implements I18nTranslationService {

    private final PostTranslationRepo postTranslationRepo;
    private final TranslationJobRepo translationJobRepo;
    private final BlogsRepo blogsRepo;
    private final BedrockPostTranslationClient bedrockClient;

    // 지원 대상 언어 (원본 ko 제외)
    private static final List<String> TARGET_LOCALES = List.of("en", "zh", "ja");

    private static final Map<String, String[]> LOCALE_INFO = Map.of(
            "ko", new String[]{"Korean", "한국어"},
            "en", new String[]{"English", "English"},
            "zh", new String[]{"Chinese", "中文"},
            "ja", new String[]{"Japanese", "日本語"}
    );

    @Override
    public void queueTranslations(Documents post) {
        String originalLocale = post.getOriginalLocale() != null ? post.getOriginalLocale() : "ko";
        LocalDateTime now = LocalDateTime.now();

        for (String targetLocale : TARGET_LOCALES) {
            if (targetLocale.equals(originalLocale)) continue;

            Optional<PostTranslation> existing = postTranslationRepo
                    .findByPostIdAndLocale(post.getId(), targetLocale);

            // 작성자가 수동 편집한 번역은 자동 재번역 스킵
            if (existing.isPresent()
                    && existing.get().getTranslatedBy() == TranslatedBy.AI_HUMAN) {
                log.info("수동 편집된 번역 스킵: postId={}, locale={}", post.getId(), targetLocale);
                continue;
            }

            // PostTranslation upsert (PENDING 상태로)
            PostTranslation translation = existing.orElse(PostTranslation.builder()
                    .postId(post.getId())
                    .locale(targetLocale)
                    .createdAt(now)
                    .build());

            translation.setStatus(TranslationStatus.PENDING);
            translation.setUpdatedAt(now);
            postTranslationRepo.save(translation);

            // 이미 처리 중인 작업이 없으면 새 Job 생성
            Optional<TranslationJob> activeJob = translationJobRepo
                    .findByPostIdAndTargetLocaleAndStatusIn(
                            post.getId(), targetLocale,
                            List.of(JobStatus.QUEUED, JobStatus.PROCESSING));

            if (activeJob.isEmpty()) {
                TranslationJob job = TranslationJob.builder()
                        .postId(post.getId())
                        .targetLocale(targetLocale)
                        .status(JobStatus.QUEUED)
                        .createdAt(now)
                        .build();
                translationJobRepo.save(job);
            }
        }
    }

    @Override
    public void processJob(TranslationJob job) {
        log.info("번역 작업 처리 시작: jobId={}, postId={}, locale={}", job.getId(), job.getPostId(), job.getTargetLocale());

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        translationJobRepo.save(job);

        try {
            Documents post = blogsRepo.findById(job.getPostId())
                    .orElseThrow(() -> new RuntimeException("Post not found: " + job.getPostId()));

            String originalLocale = post.getOriginalLocale() != null ? post.getOriginalLocale() : "ko";

            int contentLength = post.getContents() != null ? post.getContents().length() : 0;
            log.info("Bedrock 번역 API 호출: postId={}, locale={}, 원문 제목={}, 콘텐츠 크기={}자",
                    job.getPostId(), job.getTargetLocale(), post.getTitle(), contentLength);

            BedrockPostTranslationClient.TranslationResult result = bedrockClient.translate(
                    post.getTitle(),
                    post.getContents(),
                    null, // summary (Documents에 summary 필드 없으면 null)
                    originalLocale,
                    job.getTargetLocale()
            );

            LocalDateTime now = LocalDateTime.now();

            PostTranslation translation = postTranslationRepo
                    .findByPostIdAndLocale(post.getId(), job.getTargetLocale())
                    .orElse(PostTranslation.builder()
                            .postId(post.getId())
                            .locale(job.getTargetLocale())
                            .createdAt(now)
                            .build());

            translation.setTitle(result.title());
            translation.setContent(result.content());
            translation.setSummary(result.summary());
            translation.setStatus(TranslationStatus.COMPLETED);
            translation.setTranslatedBy(TranslatedBy.AI);
            translation.setAiModel(result.modelUsed());
            translation.setUpdatedAt(now);
            postTranslationRepo.save(translation);

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(now);
            log.info("번역 완료: postId={}, locale={}", job.getPostId(), job.getTargetLocale());

        } catch (Exception e) {
            log.error("번역 실패: postId={}, locale={}, error={}",
                    job.getPostId(), job.getTargetLocale(), e.getMessage());

            job.setRetryCount(job.getRetryCount() + 1);
            job.setErrorMessage(e.getMessage());

            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus(JobStatus.FAILED);

                // PostTranslation도 FAILED로 업데이트
                postTranslationRepo.findByPostIdAndLocale(job.getPostId(), job.getTargetLocale())
                        .ifPresent(t -> {
                            t.setStatus(TranslationStatus.FAILED);
                            t.setUpdatedAt(LocalDateTime.now());
                            postTranslationRepo.save(t);
                        });
            } else {
                job.setStatus(JobStatus.QUEUED); // 재시도 대기
            }
        }

        translationJobRepo.save(job);
    }

    @Override
    public TranslatedPostDTO getTranslatedPost(String postId, String locale) {
        Documents post = blogsRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        String originalLocale = post.getOriginalLocale() != null ? post.getOriginalLocale() : "ko";

        // "original" 이거나 원본 언어 요청 시 원본 반환
        boolean useOriginal = locale == null
                || "original".equals(locale)
                || locale.equals(originalLocale);

        String currentLocale;
        String title;
        String content;
        String summary = null;
        boolean isTranslated = false;
        String translatedBy = null;

        if (useOriginal) {
            currentLocale = originalLocale;
            title = post.getTitle();
            content = post.getContents();
        } else {
            // 번역본 조회
            Optional<PostTranslation> translation = postTranslationRepo
                    .findByPostIdAndLocale(postId, locale);

            if (translation.isPresent()
                    && (translation.get().getStatus() == TranslationStatus.COMPLETED
                        || translation.get().getStatus() == TranslationStatus.MANUALLY_EDITED)) {
                PostTranslation t = translation.get();
                currentLocale = locale;
                title = t.getTitle();
                content = t.getContent();
                summary = t.getSummary();
                isTranslated = true;
                translatedBy = t.getTranslatedBy() != null ? t.getTranslatedBy().name().toLowerCase() : null;
                if (TranslatedBy.AI_HUMAN.name().toLowerCase().equals(translatedBy)) {
                    translatedBy = "ai+human";
                }
            } else {
                // 번역 없으면 원본 fallback
                currentLocale = originalLocale;
                title = post.getTitle();
                content = post.getContents();
            }
        }

        // availableLocales 빌드
        List<PostTranslation> allTranslations = postTranslationRepo.findByPostId(postId);
        List<AvailableLocaleDTO> availableLocales = buildAvailableLocales(originalLocale, allTranslations);

        return TranslatedPostDTO.builder()
                .id(postId)
                .originalLocale(originalLocale)
                .currentLocale(currentLocale)
                .isTranslated(isTranslated)
                .translatedBy(translatedBy)
                .title(title)
                .content(content)
                .summary(summary)
                .availableLocales(availableLocales)
                .build();
    }

    @Override
    public List<AvailableLocaleDTO> getSupportedLocales() {
        List<AvailableLocaleDTO> locales = new ArrayList<>();
        LOCALE_INFO.forEach((code, info) -> locales.add(
                AvailableLocaleDTO.builder()
                        .code(code)
                        .name(info[0])
                        .nativeName(info[1])
                        .isOriginal("ko".equals(code))
                        .status("ko".equals(code) ? "original" : "supported")
                        .build()
        ));
        return locales;
    }

    @Override
    public TranslationStatusOverviewDTO getTranslationStatus(String postId) {
        Documents post = blogsRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        String originalLocale = post.getOriginalLocale() != null ? post.getOriginalLocale() : "ko";
        List<PostTranslation> translations = postTranslationRepo.findByPostId(postId);

        List<TranslationStatusOverviewDTO.TranslationStatusItemDTO> items = new ArrayList<>();
        for (String targetLocale : TARGET_LOCALES) {
            if (targetLocale.equals(originalLocale)) continue;

            Optional<PostTranslation> t = translations.stream()
                    .filter(tr -> targetLocale.equals(tr.getLocale()))
                    .findFirst();

            TranslationStatusOverviewDTO.TranslationStatusItemDTO item;
            if (t.isPresent()) {
                PostTranslation tr = t.get();
                String translatedByStr = tr.getTranslatedBy() != null
                        ? (tr.getTranslatedBy() == TranslatedBy.AI_HUMAN ? "ai+human" : tr.getTranslatedBy().name().toLowerCase())
                        : null;

                item = TranslationStatusOverviewDTO.TranslationStatusItemDTO.builder()
                        .locale(targetLocale)
                        .status(tr.getStatus().name().toLowerCase())
                        .translatedBy(translatedByStr)
                        .aiModel(tr.getAiModel())
                        .manuallyEditedAt(tr.getManuallyEditedAt() != null ? tr.getManuallyEditedAt().toString() : null)
                        .updatedAt(tr.getUpdatedAt() != null ? tr.getUpdatedAt().toString() : null)
                        .build();
            } else {
                item = TranslationStatusOverviewDTO.TranslationStatusItemDTO.builder()
                        .locale(targetLocale)
                        .status("none")
                        .build();
            }
            items.add(item);
        }

        return TranslationStatusOverviewDTO.builder()
                .postId(postId)
                .originalLocale(originalLocale)
                .translations(items)
                .build();
    }

    @Override
    public TranslationDetailDTO getTranslationDetail(String postId, String locale) {
        PostTranslation translation = postTranslationRepo.findByPostIdAndLocale(postId, locale)
                .orElseThrow(() -> new RuntimeException(
                        "Translation not found: postId=" + postId + ", locale=" + locale));

        String translatedByStr = translation.getTranslatedBy() != null
                ? (translation.getTranslatedBy() == TranslatedBy.AI_HUMAN ? "ai+human" : translation.getTranslatedBy().name().toLowerCase())
                : null;

        return TranslationDetailDTO.builder()
                .postId(postId)
                .locale(locale)
                .title(translation.getTitle())
                .content(translation.getContent())
                .summary(translation.getSummary())
                .status(translation.getStatus().name().toLowerCase())
                .translatedBy(translatedByStr)
                .aiModel(translation.getAiModel())
                .updatedAt(translation.getUpdatedAt() != null ? translation.getUpdatedAt().toString() : null)
                .build();
    }

    @Override
    public TranslationDetailDTO updateTranslation(String postId, String locale,
                                                   TranslationEditRequestDTO request, String userId) {
        PostTranslation translation = postTranslationRepo.findByPostIdAndLocale(postId, locale)
                .orElseThrow(() -> new RuntimeException(
                        "Translation not found: postId=" + postId + ", locale=" + locale));

        LocalDateTime now = LocalDateTime.now();
        translation.setTitle(request.getTitle());
        translation.setContent(request.getContent());
        translation.setSummary(request.getSummary());
        translation.setStatus(TranslationStatus.MANUALLY_EDITED);
        translation.setTranslatedBy(TranslatedBy.AI_HUMAN);
        translation.setManuallyEditedAt(now);
        translation.setManuallyEditedBy(userId);
        translation.setUpdatedAt(now);

        postTranslationRepo.save(translation);

        return getTranslationDetail(postId, locale);
    }

    @Override
    public RetranslateResponseDTO retranslate(String postId, String locale, boolean confirmed) {
        log.info("재번역 요청 수신: postId={}, locale={}, confirmed={}", postId, locale, confirmed);

        Optional<PostTranslation> existing = postTranslationRepo.findByPostIdAndLocale(postId, locale);

        // 수동 편집된 번역이면 확인 필요
        if (existing.isPresent()
                && existing.get().getTranslatedBy() == TranslatedBy.AI_HUMAN
                && !confirmed) {
            log.info("수동 편집 번역 확인 필요: postId={}, locale={}", postId, locale);
            return RetranslateResponseDTO.builder()
                    .warning("This translation has been manually edited. Re-translating will overwrite your changes.")
                    .requiresConfirmation(true)
                    .build();
        }

        // 재번역 큐잉
        LocalDateTime now = LocalDateTime.now();

        // PostTranslation upsert (없으면 새로 생성)
        PostTranslation translation = existing.orElse(PostTranslation.builder()
                .postId(postId)
                .locale(locale)
                .createdAt(now)
                .build());
        translation.setStatus(TranslationStatus.PENDING);
        translation.setUpdatedAt(now);
        postTranslationRepo.save(translation);

        TranslationJob job = TranslationJob.builder()
                .postId(postId)
                .targetLocale(locale)
                .status(JobStatus.QUEUED)
                .createdAt(now)
                .build();
        translationJobRepo.save(job);

        log.info("재번역 Job 큐잉 완료: postId={}, locale={}, jobId={}", postId, locale, job.getId());

        return RetranslateResponseDTO.builder()
                .requiresConfirmation(false)
                .build();
    }

    @Override
    public void retranslateAll(String postId) {
        log.info("전체 재번역 요청 수신: postId={}", postId);

        Documents post = blogsRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        String originalLocale = post.getOriginalLocale() != null ? post.getOriginalLocale() : "ko";
        LocalDateTime now = LocalDateTime.now();
        int queuedCount = 0;

        for (String targetLocale : TARGET_LOCALES) {
            if (targetLocale.equals(originalLocale)) continue;

            // PostTranslation upsert (없으면 새로 생성)
            Optional<PostTranslation> existing = postTranslationRepo.findByPostIdAndLocale(postId, targetLocale);
            PostTranslation translation = existing.orElse(PostTranslation.builder()
                    .postId(postId)
                    .locale(targetLocale)
                    .createdAt(now)
                    .build());
            translation.setStatus(TranslationStatus.PENDING);
            translation.setUpdatedAt(now);
            postTranslationRepo.save(translation);

            TranslationJob job = TranslationJob.builder()
                    .postId(postId)
                    .targetLocale(targetLocale)
                    .status(JobStatus.QUEUED)
                    .createdAt(now)
                    .build();
            translationJobRepo.save(job);
            queuedCount++;
        }

        log.info("전체 재번역 Job 큐잉 완료: postId={}, 큐잉된 언어 수={}", postId, queuedCount);
    }

    @Override
    public int queueAllExistingPosts() {
        // 발행된 글(draft=false) 전체 조회
        List<Documents> allPublished = blogsRepo.findAll().stream()
                .filter(doc -> !doc.isDraft())
                .toList();

        int queued = 0;
        for (Documents post : allPublished) {
            // 이미 번역이 존재하는 글은 스킵
            List<PostTranslation> existing = postTranslationRepo.findByPostId(post.getId());
            if (!existing.isEmpty()) continue;

            // originalLocale 기본값 설정
            if (post.getOriginalLocale() == null) {
                post.setOriginalLocale("ko");
                blogsRepo.save(post);
            }

            queueTranslations(post);
            queued++;
        }

        log.info("기존 글 일괄 번역 큐잉 완료: {} 건", queued);
        return queued;
    }

    @Override
    public int repairBrokenTranslations() {
        // 깨진 번역 = 파싱 실패로 모델 원문 응답(JSON 통째)이 content로 저장됐거나
        // 플레이스홀더가 복원되지 않은 채 남아 있는 COMPLETED 번역
        List<PostTranslation> completed = postTranslationRepo.findByStatus(TranslationStatus.COMPLETED);
        LocalDateTime now = LocalDateTime.now();
        int queued = 0;

        for (PostTranslation t : completed) {
            if (!isBrokenContent(t.getContent())) continue;

            log.info("깨진 번역 감지 → 재큐잉: postId={}, locale={}", t.getPostId(), t.getLocale());

            t.setStatus(TranslationStatus.PENDING);
            t.setUpdatedAt(now);
            postTranslationRepo.save(t);

            Optional<TranslationJob> activeJob = translationJobRepo
                    .findByPostIdAndTargetLocaleAndStatusIn(
                            t.getPostId(), t.getLocale(),
                            List.of(JobStatus.QUEUED, JobStatus.PROCESSING));
            if (activeJob.isEmpty()) {
                translationJobRepo.save(TranslationJob.builder()
                        .postId(t.getPostId())
                        .targetLocale(t.getLocale())
                        .status(JobStatus.QUEUED)
                        .createdAt(now)
                        .build());
            }
            queued++;
        }

        log.info("깨진 번역 복구 큐잉 완료: {} 건", queued);
        return queued;
    }

    // 깨진 번역 판별: content가 모델 JSON 응답 원문이거나 미복원 플레이스홀더 포함
    private boolean isBrokenContent(String content) {
        if (content == null) return false;
        String trimmed = content.strip();
        boolean looksLikeRawJson = trimmed.startsWith("{")
                && (trimmed.contains("\"title\"") || trimmed.contains("\"content\""));
        return looksLikeRawJson || content.contains("[[PH");
    }

    // availableLocales 빌드 헬퍼
    private List<AvailableLocaleDTO> buildAvailableLocales(String originalLocale,
                                                            List<PostTranslation> translations) {
        List<AvailableLocaleDTO> result = new ArrayList<>();

        // 원본 언어
        String[] originalInfo = LOCALE_INFO.getOrDefault(originalLocale, new String[]{"Korean", "한국어"});
        result.add(AvailableLocaleDTO.builder()
                .code(originalLocale)
                .name(originalInfo[0])
                .nativeName(originalInfo[1])
                .isOriginal(true)
                .status("original")
                .build());

        // 대상 언어들
        for (String locale : TARGET_LOCALES) {
            if (locale.equals(originalLocale)) continue;

            String[] info = LOCALE_INFO.getOrDefault(locale, new String[]{locale, locale});
            Optional<PostTranslation> t = translations.stream()
                    .filter(tr -> locale.equals(tr.getLocale()))
                    .findFirst();

            String status = t.map(tr -> tr.getStatus().name().toLowerCase()).orElse("none");

            result.add(AvailableLocaleDTO.builder()
                    .code(locale)
                    .name(info[0])
                    .nativeName(info[1])
                    .isOriginal(false)
                    .status(status)
                    .build());
        }

        return result;
    }
}
