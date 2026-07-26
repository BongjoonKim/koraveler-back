package server.nadeliv.discovery.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.nadeliv.discovery.dto.DiscoveryDigestResponse;
import server.nadeliv.discovery.model.entities.DestinationDigest;
import server.nadeliv.discovery.model.entities.DiscoveryJob;
import server.nadeliv.discovery.model.enums.DiscoveryJobStatus;
import server.nadeliv.discovery.repo.DestinationDigestRepo;
import server.nadeliv.discovery.repo.DiscoveryJobRepo;
import server.nadeliv.discovery.service.BedrockVideoSummarizer;
import server.nadeliv.discovery.service.DiscoveryService;
import server.nadeliv.discovery.service.YouTubeSearchClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryServiceImpl implements DiscoveryService {

    /** 다이제스트 신선도 — 이 기간 안에는 재수집하지 않고 캐시 반환 */
    private static final int FRESH_DAYS = 7;

    private static final List<DiscoveryJobStatus> ACTIVE_STATUSES =
            List.of(DiscoveryJobStatus.QUEUED, DiscoveryJobStatus.PROCESSING);

    private final DestinationDigestRepo destinationDigestRepo;
    private final DiscoveryJobRepo discoveryJobRepo;
    private final YouTubeSearchClient youTubeSearchClient;
    private final BedrockVideoSummarizer bedrockVideoSummarizer;

    @Override
    public DiscoveryDigestResponse getDigest(String query, String locale) {
        String key = normalizeKey(query);
        String loc = normalizeLocale(locale);

        DestinationDigest digest = destinationDigestRepo
                .findByDestinationKeyAndLocale(key, loc).orElse(null);
        DiscoveryJob latestJob = discoveryJobRepo
                .findFirstByDestinationKeyAndLocaleOrderByCreatedAtDesc(key, loc).orElse(null);

        return buildResponse(digest, latestJob);
    }

    @Override
    public DiscoveryDigestResponse requestCollection(String userId, String query, String locale, boolean force) {
        String key = normalizeKey(query);
        String loc = normalizeLocale(locale);

        if (key.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        DestinationDigest digest = destinationDigestRepo
                .findByDestinationKeyAndLocale(key, loc).orElse(null);

        // 신선한 다이제스트가 있으면 재수집하지 않음 (force 제외)
        if (!force && digest != null && digest.getCollectedAt() != null
                && digest.getCollectedAt().isAfter(LocalDateTime.now().minusDays(FRESH_DAYS))) {
            return buildResponse(digest, null);
        }

        // 이미 진행 중인 작업이 있으면 중복 큐잉 방지
        DiscoveryJob activeJob = discoveryJobRepo
                .findFirstByDestinationKeyAndLocaleAndStatusIn(key, loc, ACTIVE_STATUSES)
                .orElse(null);
        if (activeJob != null) {
            return buildResponse(digest, activeJob);
        }

        DiscoveryJob job = DiscoveryJob.builder()
                .destinationKey(key)
                .query(query.trim())
                .locale(loc)
                .requestedUser(userId)
                .createdAt(LocalDateTime.now())
                .build();
        discoveryJobRepo.save(job);
        log.info("Discovery 수집 큐잉: key='{}', locale={}, user={}", key, loc, userId);

        return buildResponse(digest, job);
    }

    @Override
    public void processJob(DiscoveryJob job) {
        job.setStatus(DiscoveryJobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        discoveryJobRepo.save(job);

        try {
            List<YouTubeSearchClient.VideoMeta> videoMetas =
                    youTubeSearchClient.searchTravelVideos(job.getQuery(), job.getLocale());

            DestinationDigest digest = destinationDigestRepo
                    .findByDestinationKeyAndLocale(job.getDestinationKey(), job.getLocale())
                    .orElseGet(() -> DestinationDigest.builder()
                            .destinationKey(job.getDestinationKey())
                            .locale(job.getLocale())
                            .build());
            digest.setDisplayName(job.getQuery());

            if (videoMetas.isEmpty()) {
                // 검색 결과 없음 — 빈 다이제스트로 캐싱해 쿼터 재소모 방지
                digest.setSummary(null);
                digest.setVideos(List.of());
            } else {
                BedrockVideoSummarizer.CurationResult curated =
                        bedrockVideoSummarizer.curate(job.getQuery(), job.getLocale(), videoMetas);
                digest.setSummary(curated.summary());
                digest.setVideos(curated.videos());
            }

            digest.setCollectedAt(LocalDateTime.now());
            digest.setUpdated(LocalDateTime.now());
            if (digest.getCreated() == null) digest.setCreated(LocalDateTime.now());
            destinationDigestRepo.save(digest);

            job.setStatus(DiscoveryJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            discoveryJobRepo.save(job);
            log.info("Discovery 수집 완료: key='{}', 영상 {} 건",
                    job.getDestinationKey(), digest.getVideos().size());

        } catch (Exception e) {
            handleFailure(job, e);
        }
    }

    private void handleFailure(DiscoveryJob job, Exception e) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(e.getMessage());

        if (job.getRetryCount() >= job.getMaxRetries()) {
            job.setStatus(DiscoveryJobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            log.error("Discovery 작업 최종 실패: key='{}', error={}", job.getDestinationKey(), e.getMessage());
        } else {
            job.setStatus(DiscoveryJobStatus.QUEUED);
            log.warn("Discovery 작업 실패, 재시도 예정 ({}/{}): key='{}', error={}",
                    job.getRetryCount(), job.getMaxRetries(), job.getDestinationKey(), e.getMessage());
        }
        discoveryJobRepo.save(job);
    }

    private DiscoveryDigestResponse buildResponse(DestinationDigest digest, DiscoveryJob job) {
        return DiscoveryDigestResponse.builder()
                .digest(digest)
                .jobStatus(job != null ? job.getStatus() : null)
                .errorMessage(job != null && job.getStatus() == DiscoveryJobStatus.FAILED
                        ? job.getErrorMessage() : null)
                .build();
    }

    private String normalizeKey(String query) {
        if (query == null) return "";
        return query.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) return "en";
        return switch (locale) {
            case "ko", "en", "zh", "ja" -> locale;
            default -> "en";
        };
    }
}
