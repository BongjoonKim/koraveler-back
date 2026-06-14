package server.nadeliv.users.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 이메일 발송 차단(suppression) 목록.
 *
 * SES에서 하드 바운스(Permanent)·불만(Complaint)이 발생한 주소를 영구 차단해
 * 동일 주소로의 재발송을 막는다(반복 바운스 방지 → 평판 보호).
 *
 * Redis Hash 한 곳(email:suppression)에 field=이메일, value=사유 로 저장한다.
 *  - 등록: {@link #suppress(String, String)}  (SES→SNS 웹훅에서 호출)
 *  - 확인: {@link #isSuppressed(String)}       (발송 직전 호출)
 *  - 해제: {@link #release(String)}            (오등록 복구용 — 관리자)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SuppressionStore {

    private final StringRedisTemplate redisTemplate;

    private static final String SUPPRESSION_KEY = "email:suppression";

    public void suppress(String email, String reason) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        redisTemplate.opsForHash().put(SUPPRESSION_KEY, normalized, (reason == null) ? "unknown" : reason);
        log.warn("이메일 suppression 등록: {} (사유: {})", normalized, reason);
    }

    public boolean isSuppressed(String email) {
        if (email == null || email.isBlank()) return false;
        String normalized = email.trim().toLowerCase();
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(SUPPRESSION_KEY, normalized));
    }

    public void release(String email) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        redisTemplate.opsForHash().delete(SUPPRESSION_KEY, normalized);
        log.info("이메일 suppression 해제: {}", normalized);
    }

    public Long size() {
        return redisTemplate.opsForHash().size(SUPPRESSION_KEY);
    }
}
