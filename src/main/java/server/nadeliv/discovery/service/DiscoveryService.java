package server.nadeliv.discovery.service;

import server.nadeliv.discovery.dto.DiscoveryDigestResponse;
import server.nadeliv.discovery.model.entities.DiscoveryJob;

public interface DiscoveryService {

    /** 다이제스트 조회 (비인증 허용). 수집 중이면 jobStatus 포함. */
    DiscoveryDigestResponse getDigest(String query, String locale);

    /** 수집 요청 (인증 필요). 신선한 다이제스트가 있으면 바로 반환, 없으면 작업 큐잉. */
    DiscoveryDigestResponse requestCollection(String userId, String query, String locale, boolean force);

    /** 워커가 호출하는 작업 처리. */
    void processJob(DiscoveryJob job);
}
