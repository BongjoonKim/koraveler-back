package server.nadeliv.users.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.users.service.SesNotificationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * SES 바운스/불만 알림 수신 웹훅 (SNS → 이 엔드포인트).
 *
 * 공개 경로(ps)이지만 공유 시크릿 토큰(query param t)으로 인증한다.
 * 토큰이 설정되지 않았거나 불일치하면 모든 요청을 거부한다(fail-closed) →
 * 미설정 상태에서 외부의 위조 알림으로 suppression 목록이 오염되는 것을 막는다.
 *
 * SNS는 Content-Type: text/plain 으로 본문을 전송하므로 raw String으로 받아 파싱한다.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class SesNotificationController {

    private final SesNotificationService sesNotificationService;

    @Value("${cloud.aws.ses.notifications.secret-token:}")
    private String secretToken;

    @PostMapping("/ps/ses-events")
    public ResponseEntity<?> receive(
            @RequestParam(value = "t", required = false) String token,
            @RequestBody(required = false) String body) {

        if (!isTokenValid(token)) {
            log.warn("SES 웹훅 인증 실패 (토큰 미설정 또는 불일치)");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false));
        }
        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        String result = sesNotificationService.handle(body);
        return ResponseEntity.ok(Map.of("success", true, "result", result));
    }

    /** 상수 시간 비교 (타이밍 공격 방지). 토큰 미설정 시 항상 false. */
    private boolean isTokenValid(String provided) {
        if (secretToken == null || secretToken.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                secretToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
