package server.nadeliv.users.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.time.Duration;
import java.util.Hashtable;
import java.util.regex.Pattern;

/**
 * 이메일 배달 가능성 검증 컴포넌트.
 *
 * SES 하드 바운스(존재하지 않는 주소로의 발송)의 상당수는 오타·가짜 도메인 때문이다.
 * SES 호출 "전에" 두 단계로 걸러 바운스를 사전 차단한다.
 *  1) 형식(syntax) 검증 — 보수적 정규식
 *  2) MX/A 레코드 조회 — 도메인이 실제로 메일을 받을 수 있는지 DNS로 확인
 *
 * DNS 조회 결과는 Redis에 도메인 단위로 캐싱해 지연·부하를 줄이고,
 * 조회 자체가 실패(타임아웃/네트워크 등)하면 정상 사용자 차단을 막기 위해 fail-open 처리한다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailDeliverabilityValidator {

    private final StringRedisTemplate redisTemplate;

    // 실무 수준의 보수적 이메일 형식 검증
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // MX 조회 결과 캐시
    private static final String MX_CACHE_PREFIX = "email:mx:";
    private static final Duration MX_OK_TTL = Duration.ofHours(24);   // 메일서버 있음 (안정적)
    private static final Duration MX_FAIL_TTL = Duration.ofHours(1);  // 메일서버 없음 (설정 변경 가능성 → 짧게)

    private static final String DNS_TIMEOUT_MS = "3000";

    /** 이메일 형식이 유효한가 */
    public boolean isValidSyntax(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 도메인이 메일을 수신할 수 있는가 (MX 또는 A 레코드 존재).
     * 조회 실패 시에는 fail-open(true) → 정상 사용자가 DNS 일시 장애로 막히지 않도록.
     */
    public boolean isDeliverableDomain(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }

        // 1) 캐시 확인
        String cacheKey = MX_CACHE_PREFIX + domain;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return "1".equals(cached);
        }

        // 2) DNS 조회 (MX → 없으면 A 레코드로 폴백)
        boolean deliverable;
        try {
            deliverable = hasMailRecord(domain);
        } catch (NamingException e) {
            // 조회 실패: fail-open, 캐싱하지 않음(다음에 재시도)
            log.warn("MX 조회 실패(통과 처리): domain={}, err={}", domain, e.getMessage());
            return true;
        }

        // 3) 결과 캐싱
        redisTemplate.opsForValue().set(cacheKey, deliverable ? "1" : "0",
                deliverable ? MX_OK_TTL : MX_FAIL_TTL);
        return deliverable;
    }

    private boolean hasMailRecord(String domain) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", DNS_TIMEOUT_MS);
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        InitialDirContext ctx = new InitialDirContext(env);
        try {
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX", "A"});
            boolean hasMx = attrs.get("MX") != null;
            // MX가 없어도 A 레코드가 있으면 메일 수신 가능 (RFC 5321 implicit MX)
            boolean hasA = attrs.get("A") != null;
            return hasMx || hasA;
        } finally {
            ctx.close();
        }
    }

    private String extractDomain(String email) {
        if (email == null) return null;
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return null;
        return email.substring(at + 1).toLowerCase();
    }
}
