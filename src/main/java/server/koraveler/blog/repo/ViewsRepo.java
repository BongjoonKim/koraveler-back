package server.koraveler.blog.repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import server.koraveler.blog.model.DocumentView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViewsRepo extends MongoRepository<DocumentView, String> {

    //  특정 문서의 특정 IP가 특정 시간 이후에 조회한 기록이 있는지 확인
    // 값이 없으면 24시간 이내에 조회 기록이 없음. -> 조회수 증가
    Optional<DocumentView> findByDocumentIdAndIpAddressAndViewedAtAfter(
            String documentId,
            String ipAddress,
            LocalDateTime after
    );

    // 특정 문서의 전체 조회수
    long countByDocumentId(String documentId);

    // 특정 문서의 IP 기준 조회수
    @Query(value = "{ 'documentId': ?0 }", count = true)
    long countDistinctIpByDocumentId(String documentId);

    // 특정 문서의 특정 기간 조회수
    long countByDocumentIdAndViewedAtBetween(
            String documentId,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByDocumentIdAndViewedAtAfter(String documentId, LocalDateTime after);

    // 특정 문서의 모든 조회 기록 (통계용)
    List<DocumentView> findByDocumentIdOrderByViewedAtDesc(String documentId);

    // 특정 문서의 유니크 IP 목록 조회
    @Query(value = "{ 'documentId': ?0 }", fields = "{ 'ipAddress': 1 }")
    List<DocumentView> findDistinctIpAddressesByDocumentId(String documentId);
}
