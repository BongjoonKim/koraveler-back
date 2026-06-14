package server.nadeliv.users.service.EmailVerificationServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import server.nadeliv.common.service.EmailService;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
import server.nadeliv.users.component.EmailDeliverabilityValidator;
import server.nadeliv.users.component.SuppressionStore;
import server.nadeliv.users.service.EmailVerificationService;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final EmailDeliverabilityValidator emailValidator;
    private final SuppressionStore suppressionStore;

    // 요청자 추적 전용 감사 로거 — CloudWatch Logs Insights에서 logger="EMAIL_AUDIT" 로 필터링
    private static final Logger auditLog = LoggerFactory.getLogger("EMAIL_AUDIT");

    // Redis 키 접두사
    private static final String CODE_PREFIX = "email:verify:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final String SEND_COUNT_PREFIX = "email:send-count:";   // 이메일당 24h 발송 횟수
    private static final String FAIL_COUNT_PREFIX = "email:fail-count:";   // 이메일당 검증 실패 횟수
    private static final String COOLDOWN_PREFIX = "email:cooldown:";       // 이메일당 재발송 쿨다운
    private static final String IP_HOUR_PREFIX = "email:ip-hour:";         // IP당 시간당 발송
    private static final String IP_DAY_PREFIX = "email:ip-day:";          // IP당 일일 발송
    private static final String IP_EMAILS_PREFIX = "email:ip-emails:";    // IP당 일일 distinct 이메일 (Set)

    // TTL 설정
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final Duration SEND_COUNT_TTL = Duration.ofHours(24);
    private static final Duration FAIL_COUNT_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final Duration IP_HOUR_TTL = Duration.ofHours(1);
    private static final Duration IP_DAY_TTL = Duration.ofHours(24);
    private static final long IP_EMAILS_TTL_SEC = Duration.ofHours(24).toSeconds();

    // 제한 횟수 (균형 강도)
    private static final int MAX_SEND_COUNT = 5;     // 이메일당 24시간 발송
    private static final int MAX_FAIL_COUNT = 10;    // 이메일당 검증 실패
    private static final int MAX_IP_HOUR = 5;        // IP당 시간당 발송
    private static final int MAX_IP_DAY = 20;        // IP당 일일 발송
    private static final int MAX_IP_DISTINCT = 10;   // IP당 일일 서로 다른 이메일 수 (enumeration 방지)

    @Override
    public void sendCode(String email, String clientIp, String userAgent) {
        String normalized = (email == null) ? "" : email.trim().toLowerCase();
        String domain = normalized.contains("@") ? normalized.substring(normalized.indexOf('@') + 1) : "-";

        // 0. 형식 검증 — 배달 불가 주소를 SES 호출 전에 차단 (바운스 예방)
        if (!emailValidator.isValidSyntax(normalized)) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_INVALID_SYNTAX", "형식 오류");
            throw new CustomException(ErrorCode.EMAIL_INVALID_FORMAT);
        }

        // 0.5 suppression 차단 — 과거 하드 바운스/불만 발생 주소는 재발송하지 않음 (반복 바운스 방지)
        if (suppressionStore.isSuppressed(normalized)) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_SUPPRESSED", "suppression 목록");
            throw new CustomException(ErrorCode.EMAIL_UNDELIVERABLE);
        }

        // 1. 재발송 쿨다운 (이메일당 60초) — 동일 주소 연타 방지
        String cooldownKey = COOLDOWN_PREFIX + normalized;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_COOLDOWN", "재발송 쿨다운");
            throw new CustomException(ErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        // 2. IP당 시간/일 발송 한도 — 단일 출처의 대량 발송 차단
        String ipHourKey = IP_HOUR_PREFIX + clientIp;
        String ipDayKey = IP_DAY_PREFIX + clientIp;
        int ipHour = parseCount(ipHourKey);
        int ipDay = parseCount(ipDayKey);
        if (ipHour >= MAX_IP_HOUR || ipDay >= MAX_IP_DAY) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_IP_RATE",
                    "ipHour=" + ipHour + " ipDay=" + ipDay);
            throw new CustomException(ErrorCode.EMAIL_IP_RATE_EXCEEDED);
        }

        // 3. IP당 distinct 이메일 한도 — 한 IP에서 가짜 주소를 enumeration 하는 봇 차단
        String ipEmailsKey = IP_EMAILS_PREFIX + clientIp;
        Boolean alreadyTried = redisTemplate.opsForSet().isMember(ipEmailsKey, normalized);
        Long distinctSize = redisTemplate.opsForSet().size(ipEmailsKey);
        long distinct = (distinctSize != null) ? distinctSize : 0L;
        if (!Boolean.TRUE.equals(alreadyTried) && distinct >= MAX_IP_DISTINCT) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_IP_DISTINCT", "distinct=" + distinct);
            throw new CustomException(ErrorCode.EMAIL_IP_DISTINCT_EXCEEDED);
        }

        // 4. 이메일당 24시간 발송 한도
        String sendCountKey = SEND_COUNT_PREFIX + normalized;
        int currentCount = parseCount(sendCountKey);
        if (currentCount >= MAX_SEND_COUNT) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_EMAIL_LIMIT", "count=" + currentCount);
            throw new CustomException(ErrorCode.EMAIL_SEND_LIMIT_EXCEEDED, "24시간 후 다시 시도해주세요.");
        }

        // 5. MX(도메인 메일서버) 검증 — DNS 조회는 위의 저렴한 검사를 모두 통과한 뒤에만 수행
        if (!emailValidator.isDeliverableDomain(normalized)) {
            audit(clientIp, userAgent, normalized, domain, "BLOCKED_NO_MX", "수신 불가 도메인");
            throw new CustomException(ErrorCode.EMAIL_UNDELIVERABLE);
        }

        // 6. 인증 코드 생성 및 저장 (TTL 5분), 검증 실패 횟수 초기화
        String code = generateCode();
        redisTemplate.opsForValue().set(CODE_PREFIX + normalized, code, CODE_TTL);
        redisTemplate.delete(FAIL_COUNT_PREFIX + normalized);

        // 7. 카운터/쿨다운 갱신
        incrementWithTtl(sendCountKey, currentCount, SEND_COUNT_TTL);
        incrementWithTtl(ipHourKey, ipHour, IP_HOUR_TTL);
        incrementWithTtl(ipDayKey, ipDay, IP_DAY_TTL);
        redisTemplate.opsForSet().add(ipEmailsKey, normalized);
        redisTemplate.expire(ipEmailsKey, IP_EMAILS_TTL_SEC, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_TTL);

        // 8. 이메일 발송
        emailService.sendVerificationCode(normalized, code);
        audit(clientIp, userAgent, normalized, domain, "SENT",
                "emailCount=" + (currentCount + 1) + " ipHour=" + (ipHour + 1) + " ipDay=" + (ipDay + 1));
        log.info("인증 코드 발송 완료: {} (이메일 발송횟수: {}, IP: {})", normalized, currentCount + 1, clientIp);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String normalized = (email == null) ? "" : email.trim().toLowerCase();
        String codeKey = CODE_PREFIX + normalized;
        String failCountKey = FAIL_COUNT_PREFIX + normalized;

        // 1. 검증 실패 횟수 확인 (초과 시 코드 무효화)
        int failCount = parseCount(failCountKey);
        if (failCount >= MAX_FAIL_COUNT) {
            redisTemplate.delete(codeKey);
            log.warn("인증 코드 검증 실패 횟수 초과로 코드 무효화: {}", normalized);
            throw new CustomException(ErrorCode.EMAIL_VERIFY_ATTEMPTS_EXCEEDED,
                    "새로운 인증 코드를 요청해주세요.");
        }

        // 2. 저장된 코드 조회
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            log.warn("인증 코드 만료 또는 미존재: {}", normalized);
            throw new CustomException(ErrorCode.EMAIL_CODE_EXPIRED);
        }

        // 3. 코드 비교
        if (!storedCode.equals(code)) {
            incrementWithTtl(failCountKey, failCount, FAIL_COUNT_TTL);
            log.warn("인증 코드 불일치: {} (실패 횟수: {})", normalized, failCount + 1);
            return false;
        }

        // 4. 인증 성공 → 인증 완료 플래그 저장 (TTL 30분)
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + normalized, "true", VERIFIED_TTL);

        // 5. 사용된 코드 및 실패 횟수 삭제
        redisTemplate.delete(codeKey);
        redisTemplate.delete(failCountKey);

        log.info("이메일 인증 완료: {}", normalized);
        return true;
    }

    @Override
    public boolean isVerified(String email) {
        String normalized = (email == null) ? "" : email.trim().toLowerCase();
        String value = redisTemplate.opsForValue().get(VERIFIED_PREFIX + normalized);
        return "true".equals(value);
    }

    @Override
    public void clearVerified(String email) {
        String normalized = (email == null) ? "" : email.trim().toLowerCase();
        redisTemplate.delete(VERIFIED_PREFIX + normalized);
        log.info("이메일 인증 플래그 삭제: {}", normalized);
    }

    /**
     * 6자리 숫자 인증 코드 생성
     */
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Redis 카운터 값 조회 (없으면 0)
     */
    private int parseCount(String key) {
        String v = redisTemplate.opsForValue().get(key);
        return (v != null) ? Integer.parseInt(v) : 0;
    }

    /**
     * 카운터 증가. 최초 생성 시에만 TTL을 설정해 윈도우가 갱신되지 않도록 한다.
     */
    private void incrementWithTtl(String key, int currentCount, Duration ttl) {
        if (currentCount == 0) {
            redisTemplate.opsForValue().set(key, "1", ttl);
        } else {
            redisTemplate.opsForValue().increment(key);
        }
    }

    /**
     * 발송 시도 감사 로그. 전용 로거(EMAIL_AUDIT)로 구조화(key=value)하여 기록한다.
     * CloudWatch Logs Insights에서 ip/action/reason 기준으로 "누가 얼마나 요청했는지" 추적 가능.
     */
    private void audit(String ip, String ua, String email, String domain, String action, String reason) {
        auditLog.info("action={} ip={} email={} domain={} reason=\"{}\" ua=\"{}\"",
                action, ip, email, domain, reason, ua);
    }
}
