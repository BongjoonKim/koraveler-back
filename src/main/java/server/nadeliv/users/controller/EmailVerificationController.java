package server.nadeliv.users.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.users.dto.EmailCodeVerifyRequest;
import server.nadeliv.users.dto.EmailVerificationRequest;
import server.nadeliv.users.service.EmailVerificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * 인증 코드 발송 (비인증, ps 경로)
     */
    @PostMapping("/ps/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendCode(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "인증 코드가 발송되었습니다."
        ));
    }

    /**
     * 인증 코드 검증 (비인증, ps 경로)
     */
    @PostMapping("/ps/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody EmailCodeVerifyRequest request) {
        boolean verified = emailVerificationService.verifyCode(request.getEmail(), request.getCode());

        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이메일 인증이 완료되었습니다."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증 코드가 일치하지 않습니다."
            ));
        }
    }
}
