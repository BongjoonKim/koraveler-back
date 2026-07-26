package server.nadeliv.discovery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.nadeliv.discovery.model.entities.DiscoveryJob;
import server.nadeliv.discovery.model.enums.DiscoveryJobStatus;
import server.nadeliv.discovery.repo.DiscoveryJobRepo;

import java.util.List;

/**
 * 목적지 정보 수집 폴링 워커. TranslationWorker 와 동일한 패턴.
 * YouTube 쿼터(검색 1회 = 100 units)를 고려해 한 번에 최대 3건만 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryWorker {

    private final DiscoveryJobRepo discoveryJobRepo;
    private final DiscoveryService discoveryService;

    @Scheduled(fixedDelay = 5000)
    public void processQueuedJobs() {
        List<DiscoveryJob> jobs = discoveryJobRepo
                .findTop3ByStatusOrderByCreatedAtAsc(DiscoveryJobStatus.QUEUED);

        if (jobs.isEmpty()) return;

        log.info("Discovery Worker: QUEUED 작업 {} 건 감지, 처리 시작", jobs.size());

        for (DiscoveryJob job : jobs) {
            try {
                discoveryService.processJob(job);
            } catch (Exception e) {
                log.error("Discovery Worker: 예상치 못한 에러 - jobId={}, key={}, error={}",
                        job.getId(), job.getDestinationKey(), e.getMessage(), e);
            }
        }
    }
}
