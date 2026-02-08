package server.koraveler.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.ViewDTO;
import server.koraveler.blog.dto.ViewStatsDTO;
import server.koraveler.blog.model.DocumentView;
import server.koraveler.blog.repo.ViewsRepo;
import server.koraveler.blog.service.ViewService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViewServiceImpl implements ViewService {
    private final ViewsRepo viewsRepo;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis key
    private static final String VIEW_CHECK_PREFIX = "view:check:";
    private static final String VIEW_COUNT_PREFIX = "view:count:";

    // 중복 시간 방지
    private static final Duration DUPLICATE_PREVENTION_TTL = Duration.ofHours(24);
    private static final Duration VIEW_COUNT_CACHE_TTL = Duration.ofMinutes(5);

    @Override
    public boolean incrementView(ViewDTO viewDTO) {
        try {
            String documentId = viewDTO.getDocumentId();
            String ipAddress = viewDTO.getIpAddress();

            // Redis Key: view:check:{documentId}:{ipAddress}
            String checkKey = VIEW_CHECK_PREFIX + documentId + ":" + ipAddress;

            // Redis에서 중복 체크 (24시간 내 동일 IP 조회 여부)
            Boolean isFirstView = redisTemplate.opsForValue().setIfAbsent(
                    checkKey,
                    "1",
                    DUPLICATE_PREVENTION_TTL
            );

            if (Boolean.FALSE.equals(isFirstView)) {
                log.debug("중복 조회 (Redis) - documentId: {}, ip: {}", documentId, ipAddress);
                return false;
            }

            // MongoDB에 조회 기록 저장
            DocumentView newView = DocumentView.builder()
                    .documentId(documentId)
                    .ipAddress(ipAddress)
                    .userAgent(viewDTO.getUserAgent())
                    .referer(viewDTO.getReferer())
                    .viewedAt(LocalDateTime.now())
                    .deviceType(parseDeviceType(viewDTO.getUserAgent()))
                    .build();

            viewsRepo.save(newView);

            // Redis 조회수 캐시 증가
            String countKey = VIEW_COUNT_PREFIX + documentId;
            redisTemplate.opsForValue().increment(countKey);

            log.info("조회수 증가 - documentId: {}, ip: {}", documentId, ipAddress);
            return true;
        } catch (Exception error) {
            log.error("조회수 증가 실패 - documentId: {}", viewDTO.getDocumentId(), error);
            return false;
        }
    }

    @Override
    public long getTotalViews(String documentId) {
        String countKey = VIEW_COUNT_PREFIX + documentId;

        // Redis 캐시 확인
        String cachedCount = redisTemplate.opsForValue().get(countKey);
        if (cachedCount != null) {
            return Long.parseLong(cachedCount);
        }

        // 캐시 미스: MongoDB에서 조회 후 캐싱
        long count = viewsRepo.countByDocumentId(documentId);
        redisTemplate.opsForValue().set(countKey, String.valueOf(count), VIEW_COUNT_CACHE_TTL);

        return count;    }

    @Override
    public long getUniqueViews(String documentId) {
        String cacheKey = "view:unique:" + documentId;

        // Redis 캐시 확인
        String cachedCount = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            return Long.parseLong(cachedCount);
        }

        // Aggregation으로 유니크 IP 수 계산
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("documentId").is(documentId)),
                Aggregation.group("ipAddress"),
                Aggregation.count().as("count")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "document_views", Map.class
        );

        long uniqueCount = results.getMappedResults().size();

        // 캐싱 (5분)
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(uniqueCount), VIEW_COUNT_CACHE_TTL);

        return uniqueCount;
    }

    @Override
    public long getTodayViews(String documentId) {
        String cacheKey = "view:today:" + documentId;

        // Redis 캐시 확인
        String cachedCount = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            return Long.parseLong(cachedCount);
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long count = viewsRepo.countByDocumentIdAndViewedAtBetween(documentId, startOfDay, endOfDay);

        // 캐싱 (1분 - 오늘 조회수는 자주 변경됨)
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(count), Duration.ofMinutes(1));

        return count;
    }

    @Override
    public long getWeekViews(String documentId) {
        String cacheKey = "view:week:" + documentId;

        // Redis 캐시 확인
        String cachedCount = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            return Long.parseLong(cachedCount);
        }

        LocalDateTime oneWeekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        long count = viewsRepo.countByDocumentIdAndViewedAtAfter(documentId, oneWeekAgo);

        // 캐싱 (5분)
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(count), VIEW_COUNT_CACHE_TTL);

        return count;
    }

    @Override
    public ViewStatsDTO getViewStats(String documentId) {
        long totalViews = getTotalViews(documentId);
        long uniqueViews = getUniqueViews(documentId);
        long todayViews = getTodayViews(documentId);
        long weekViews = getWeekViews(documentId);

        // 일별 통계 (최근 7일)
        List<ViewStatsDTO.DailyViewCount> dailyStats = getDailyStats(documentId, 7);

        return ViewStatsDTO.builder()
                .documentId(documentId)
                .totalViews(totalViews)
                .uniqueViews(uniqueViews)
                .todayViews(todayViews)
                .weekViews(weekViews)
                .dailyStats(dailyStats)
                .build();
    }

    /**
     * 일별 조회 통계 조회
     */
    private List<ViewStatsDTO.DailyViewCount> getDailyStats(String documentId, int days) {
        List<ViewStatsDTO.DailyViewCount> dailyStats = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            long count = viewsRepo.countByDocumentIdAndViewedAtBetween(
                    documentId, startOfDay, endOfDay
            );

            dailyStats.add(new ViewStatsDTO.DailyViewCount(
                    date.toString(),
                    count
            ));
        }

        // 날짜 오름차순 정렬
        Collections.reverse(dailyStats);
        return dailyStats;
    }

    /**
     * User-Agent를 파싱하여 디바이스 타입 반환
     */
    private String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }

        String lowerUserAgent = userAgent.toLowerCase();

        if (lowerUserAgent.contains("mobile") || lowerUserAgent.contains("android")
                || lowerUserAgent.contains("iphone")) {
            return "mobile";
        } else if (lowerUserAgent.contains("tablet") || lowerUserAgent.contains("ipad")) {
            return "tablet";
        } else {
            return "desktop";
        }
    }
}
