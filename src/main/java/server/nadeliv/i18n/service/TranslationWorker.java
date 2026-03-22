package server.nadeliv.i18n.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.nadeliv.i18n.model.entities.TranslationJob;
import server.nadeliv.i18n.model.enums.JobStatus;
import server.nadeliv.i18n.repo.TranslationJobRepo;

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

        log.info("번역 Worker: QUEUED 작업 {} 건 감지, 처리 시작", jobs.size());

        for (TranslationJob job : jobs) {
            try {
                i18nTranslationService.processJob(job);
            } catch (Exception e) {
                log.error("번역 Worker: 예상치 못한 에러 발생 - jobId={}, postId={}, locale={}, error={}",
                        job.getId(), job.getPostId(), job.getTargetLocale(), e.getMessage(), e);
            }
        }

        log.info("번역 Worker: {} 건 처리 완료", jobs.size());
    }
}
