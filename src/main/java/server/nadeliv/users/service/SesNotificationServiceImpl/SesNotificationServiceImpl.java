package server.nadeliv.users.service.SesNotificationServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
import server.nadeliv.users.component.SuppressionStore;
import server.nadeliv.users.service.SesNotificationService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * SES → SNS 바운스/불만 알림 처리.
 *
 * 보안: 엔드포인트는 공개(ps)이므로 컨트롤러의 공유 시크릿 토큰으로 1차 인증하고,
 * 여기서 추가로 (1) TopicArn 화이트리스트, (2) SigningCertURL 호스트 검증을 수행한다.
 * (SNS 서명 검증까지 추가하면 더 강력하지만, 현재는 시크릿 토큰 + 위 방어로 충분.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SesNotificationServiceImpl implements SesNotificationService {

    private final SuppressionStore suppressionStore;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.ses.notifications.allowed-topic-arn:}")
    private String allowedTopicArn;

    // SNS 서명 인증서 URL은 sns.<region>.amazonaws.com 형태여야 한다 (SSRF 방어)
    private static final Pattern SNS_CERT_HOST = Pattern.compile("^sns\\.[a-z0-9-]+\\.amazonaws\\.com$");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String handle(String rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("SNS 메시지 파싱 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_INPUT, "잘못된 SNS 페이로드");
        }

        String type = text(root, "Type");
        String topicArn = text(root, "TopicArn");

        // 1) TopicArn 화이트리스트 (설정된 경우에만 검사)
        if (allowedTopicArn != null && !allowedTopicArn.isBlank()
                && !allowedTopicArn.equals(topicArn)) {
            log.warn("허용되지 않은 SNS TopicArn: {}", topicArn);
            throw new CustomException(ErrorCode.FORBIDDEN, "허용되지 않은 토픽");
        }

        // 2) SigningCertURL 호스트 검증 (있을 때만)
        String certUrl = text(root, "SigningCertURL");
        if (certUrl != null && !isAwsCertUrl(certUrl)) {
            log.warn("의심스러운 SigningCertURL: {}", certUrl);
            throw new CustomException(ErrorCode.FORBIDDEN, "잘못된 서명 인증서 URL");
        }

        if ("SubscriptionConfirmation".equals(type)) {
            return confirmSubscription(root);
        }
        if ("Notification".equals(type)) {
            return processNotification(root);
        }
        log.info("처리 대상 아닌 SNS Type: {}", type);
        return "ignored:" + type;
    }

    /**
     * SNS 구독 확인: SubscribeURL을 GET 호출하면 구독이 확정된다.
     */
    private String confirmSubscription(JsonNode root) {
        String subscribeUrl = text(root, "SubscribeURL");
        if (subscribeUrl == null || !isAwsHttpsUrl(subscribeUrl)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "잘못된 SubscribeURL");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(subscribeUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("SNS 구독 확인 완료: status={}", resp.statusCode());
            return "subscription-confirmed:" + resp.statusCode();
        } catch (Exception e) {
            log.error("SNS 구독 확인 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "구독 확인 실패");
        }
    }

    /**
     * 실제 알림 처리. Bounce(Permanent)·Complaint 발생 주소를 suppression 목록에 등록한다.
     * identity notification(notificationType)과 event publishing(eventType) 형식을 모두 지원.
     */
    private String processNotification(JsonNode root) {
        String messageStr = text(root, "Message");
        if (messageStr == null) {
            return "no-message";
        }
        JsonNode msg;
        try {
            msg = objectMapper.readTree(messageStr);
        } catch (Exception e) {
            log.warn("SES Message 파싱 실패: {}", e.getMessage());
            return "bad-message";
        }

        String eventType = msg.has("notificationType") ? msg.get("notificationType").asText()
                : msg.has("eventType") ? msg.get("eventType").asText()
                : "Unknown";

        int suppressed = 0;

        if ("Bounce".equalsIgnoreCase(eventType)) {
            JsonNode bounce = msg.get("bounce");
            String bounceType = (bounce != null) ? text(bounce, "bounceType") : null;
            // Permanent(하드 바운스)만 영구 차단. Transient(일시적)는 제외.
            if (bounce != null && "Permanent".equalsIgnoreCase(bounceType)) {
                String subType = text(bounce, "bounceSubType");
                for (JsonNode r : bounce.path("bouncedRecipients")) {
                    String email = text(r, "emailAddress");
                    if (email != null) {
                        suppressionStore.suppress(email, "bounce:Permanent:" + subType);
                        suppressed++;
                    }
                }
            }
            log.info("SES Bounce 수신: type={} suppressed={}", bounceType, suppressed);

        } else if ("Complaint".equalsIgnoreCase(eventType)) {
            JsonNode complaint = msg.get("complaint");
            if (complaint != null) {
                for (JsonNode r : complaint.path("complainedRecipients")) {
                    String email = text(r, "emailAddress");
                    if (email != null) {
                        suppressionStore.suppress(email, "complaint");
                        suppressed++;
                    }
                }
            }
            log.info("SES Complaint 수신: suppressed={}", suppressed);

        } else {
            log.info("SES 알림 수신(차단 대상 아님): eventType={}", eventType);
        }

        return eventType + ":suppressed=" + suppressed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = (node != null) ? node.get(field) : null;
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private boolean isAwsCertUrl(String url) {
        try {
            URI u = URI.create(url);
            return "https".equalsIgnoreCase(u.getScheme())
                    && u.getHost() != null
                    && SNS_CERT_HOST.matcher(u.getHost()).matches();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAwsHttpsUrl(String url) {
        try {
            URI u = URI.create(url);
            return "https".equalsIgnoreCase(u.getScheme())
                    && u.getHost() != null
                    && u.getHost().endsWith(".amazonaws.com");
        } catch (Exception e) {
            return false;
        }
    }
}
