package server.nadeliv.users.service.EmailVerificationServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import server.nadeliv.common.service.EmailService;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
import server.nadeliv.users.service.EmailVerificationService;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    // Redis 키 접두사
    private static final String CODE_PREFIX = "email:verify:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final String SEND_COUNT_PREFIX = "email:send-count:";
    private static final String FAIL_COUNT_PREFIX = "email:fail-count:";

    // TTL 설정
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final Duration SEND_COUNT_TTL = Duration.ofHours(24);
    private static final Duration FAIL_COUNT_TTL = Duration.ofMinutes(5);

    // 제한 횟수
    private static final int MAX_SEND_COUNT = 10;
    private static final int MAX_FAIL_COUNT = 10;

    @Override
    public void sendCode(String email) {
        // 1. 발송 횟수 제한 확인 (24시간 내 5회)
        String sendCountKey = SEND_COUNT_PREFIX + email;
        String countStr = redisTemplate.opsForValue().get(sendCountKey);
        int currentCount = (countStr != null) ? Integer.parseInt(countStr) : 0;

        if (currentCount >= MAX_SEND_COUNT) {
            log.warn("인증 코드 발송 횟수 초과: {}", email);
            throw new CustomException(ErrorCode.EMAIL_SEND_LIMIT_EXCEEDED,
                    "24시간 후 다시 시도해주세요.");
        }

        // 2. 6자리 랜덤 코드 생성
        String code = generateCode();

        // 3. Redis에 인증 코드 저장 (TTL 5분)
        String codeKey = CODE_PREFIX + email;
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);

        // 4. 검증 실패 횟수 초기화 (새 코드 발급 시)
        String failCountKey = FAIL_COUNT_PREFIX + email;
        redisTemplate.delete(failCountKey);

        // 5. 발송 횟수 증가
        if (currentCount == 0) {
            redisTemplate.opsForValue().set(sendCountKey, "1", SEND_COUNT_TTL);
        } else {
            redisTemplate.opsForValue().increment(sendCountKey);
        }

        // 6. 이메일 발송
        emailService.sendVerificationCode(email, code);
        log.info("인증 코드 발송 완료: {} (발송 횟수: {})", email, currentCount + 1);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String codeKey = CODE_PREFIX + email;
        String failCountKey = FAIL_COUNT_PREFIX + email;

        // 1. 검증 실패 횟수 확인 (5회 초과 시 코드 무효화)
        String failCountStr = redisTemplate.opsForValue().get(failCountKey);
        int failCount = (failCountStr != null) ? Integer.parseInt(failCountStr) : 0;

        if (failCount >= MAX_FAIL_COUNT) {
            redisTemplate.delete(codeKey);
            log.warn("인증 코드 검증 실패 횟수 초과로 코드 무효화: {}", email);
            throw new CustomException(ErrorCode.EMAIL_VERIFY_ATTEMPTS_EXCEEDED,
                    "새로운 인증 코드를 요청해주세요.");
        }

        // 2. 저장된 코드 조회
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            log.warn("인증 코드 만료 또는 미존재: {}", email);
            throw new CustomException(ErrorCode.EMAIL_CODE_EXPIRED);
        }

        // 3. 코드 비교
        if (!storedCode.equals(code)) {
            if (failCount == 0) {
                redisTemplate.opsForValue().set(failCountKey, "1", FAIL_COUNT_TTL);
            } else {
                redisTemplate.opsForValue().increment(failCountKey);
            }
            log.warn("인증 코드 불일치: {} (실패 횟수: {})", email, failCount + 1);
            return false;
        }

        // 4. 인증 성공 → 인증 완료 플래그 저장 (TTL 30분)
        String verifiedKey = VERIFIED_PREFIX + email;
        redisTemplate.opsForValue().set(verifiedKey, "true", VERIFIED_TTL);

        // 5. 사용된 코드 및 실패 횟수 삭제
        redisTemplate.delete(codeKey);
        redisTemplate.delete(failCountKey);

        log.info("이메일 인증 완료: {}", email);
        return true;
    }

    @Override
    public boolean isVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        String value = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equals(value);
    }

    @Override
    public void clearVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        redisTemplate.delete(verifiedKey);
        log.info("이메일 인증 플래그 삭제: {}", email);
    }

    /**
     * 6자리 숫자 인증 코드 생성
     */
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
