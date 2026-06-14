package server.nadeliv.users.service;

public interface EmailVerificationService {
    // 인증 코드 발송 (요청자 IP/User-Agent 포함 — rate limit & 감사 로깅용)
    void sendCode(String email, String clientIp, String userAgent);

    // 인증 코드 검증
    boolean verifyCode(String email, String code);

    // 이메일 인증 완료 여부 확인 (회원가입 시 호출)
    boolean isVerified(String email);

    // 인증 완료 플래그 삭제 (회원가입 성공 후 호출)
    void clearVerified(String email);
}
