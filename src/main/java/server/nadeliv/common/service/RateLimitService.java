package server.nadeliv.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 사용자별 "하루 N회" 남용 방지용 공용 레이트 리미터.
 *
 * <p>블로그 발행·Travel 파일 업로드 등 특정 사용자가 짧은 시간에 대량 요청을 보내 서버에
 * 부하를 주는 것을 막기 위한 도메인 무관 카운터. Redis 키에 KST 날짜(yyyy-MM-dd)를 포함해
 * 자정(Asia/Seoul)에 자연스럽게 초기화되며, TTL 은 죽은 키 정리용이다.
 *
 * <p><b>Fail-open 정책</b>: Redis 장애 시 예외를 삼키고 요청을 허용한다. 레이트 리밋은
 * best-effort 보호막이며, Redis 가 잠깐 흔들린다고 정상 사용자의 글 저장·업로드 같은
 * 핵심 기능이 막혀서는 안 되기 때문이다. (블로그 번역 큐잉 실패 시 저장은 성공시키는 기존 정책과 동일 기조)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "ratelimit:";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    // 날짜 기반 키가 실제 초기화를 담당하므로 TTL 은 지난 날짜 키 청소 목적(2일).
    private static final Duration DAILY_KEY_TTL = Duration.ofDays(2);

    /**
     * 현재 사용자의 오늘 사용량이 limit 이상이면 true (즉, 한 번 더 하면 한도 초과).
     * Redis 장애 시 false(허용)로 fail-open.
     */
    public boolean isDailyLimitReached(String scope, String userId, int limit) {
        if (!StringUtils.hasText(userId)) {
            return false; // 인증은 상위 레이어에서 강제. 식별자 없으면 카운트 불가 → 통과.
        }
        try {
            String v = redisTemplate.opsForValue().get(buildKey(scope, userId));
            long current = (v == null) ? 0L : Long.parseLong(v);
            return current >= limit;
        } catch (Exception e) {
            log.warn("RateLimit 조회 실패(fail-open, 허용): scope={}, userId={}, err={}", scope, userId, e.getMessage());
            return false;
        }
    }

    /**
     * 오늘 사용량을 원자적으로 1 증가시키고 증가 후 값을 반환. 성공한 작업 직후 호출한다.
     * 최초 증가 시에만 TTL 을 설정한다. Redis 장애 시 0 을 반환하고 무시(fail-open).
     */
    public long incrementDaily(String scope, String userId) {
        if (!StringUtils.hasText(userId)) {
            return 0L;
        }
        try {
            String key = buildKey(scope, userId);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, DAILY_KEY_TTL);
            }
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("RateLimit 증가 실패(fail-open, 무시): scope={}, userId={}, err={}", scope, userId, e.getMessage());
            return 0L;
        }
    }

    private String buildKey(String scope, String userId) {
        // ratelimit:{scope}:{userId}:{yyyy-MM-dd(KST)}
        return KEY_PREFIX + scope + ":" + userId + ":" + LocalDate.now(ZONE);
    }
}
