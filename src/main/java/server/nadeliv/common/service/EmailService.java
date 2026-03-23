package server.nadeliv.common.service;

public interface EmailService {
    // 인증 코드 이메일 발송
    void sendVerificationCode(String toEmail, String code);
}
