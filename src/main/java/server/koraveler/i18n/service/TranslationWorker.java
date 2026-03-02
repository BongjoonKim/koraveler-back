package server.koraveler.i18n.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.koraveler.i18n.model.entities.TranslationJob;
import server.koraveler.i18n.model.enums.JobStatus;
import server.koraveler.i18n.repo.TranslationJobRepo;

import java.util.List;

/**
 * 번역 작업 폴링 워커.
 * 5초마다 QUEUED 상태의 작업을 최대 10개씩 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationWorker {

    private final TranslationJobRepo translationJobRepo;
    private final I18nTranslationService i18nTranslationService;

    @Scheduled(fixedDelay = 5000)
    public void processQueuedJobs() {
        List<TranslationJob> jobs = translationJobRepo
                .findTop10ByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);

        if (jobs.isEmpty()) return;

        log.debug("번역 작업 {} 건 처리 시작", jobs.size());

        for (TranslationJob job : jobs) {
            try {
                i18nTranslationService.processJob(job);
            } catch (Exception e) {
                log.error("번역 작업 처리 중 예상치 못한 에러: jobId={}, error={}",
                        job.getId(), e.getMessage());
            }
        }
    }
}
