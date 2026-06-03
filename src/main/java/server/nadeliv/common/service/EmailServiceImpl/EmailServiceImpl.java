package server.nadeliv.common.service.EmailServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.nadeliv.common.service.EmailService;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private SesClient sesClient;

    @Value("${cloud.aws.ses.from-email}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        String subject = "[Koraveler] 이메일 인증 코드 / Email Verification Code";
        String htmlBody = buildVerificationHtml(code);

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset("UTF-8")
                                            .data(htmlBody)
                                            .build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("인증 코드 이메일 발송 완료: {}", toEmail);
        } catch (SesException e) {
            log.error("이메일 발송 실패: {} - {}", toEmail, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    private String buildVerificationHtml(String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        <!-- Header -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#4F46E5,#7C3AED);padding:32px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:24px;">Koraveler</h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:14px;">이메일 인증 / Email Verification</p>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding:40px 32px;">
                            <p style="margin:0 0 24px;color:#333;font-size:15px;line-height:1.6;">
                              안녕하세요! 아래 인증 코드를 입력해주세요.<br>
                              <span style="color:#666;font-size:13px;">Please enter the verification code below.</span>
                            </p>
                            <div style="background:#F5F3FF;border:2px dashed #7C3AED;border-radius:8px;padding:20px;text-align:center;margin:0 0 24px;">
                              <span style="font-size:36px;font-weight:bold;letter-spacing:12px;color:#4F46E5;">%s</span>
                            </div>
                            <p style="margin:0;color:#999;font-size:13px;text-align:center;">
                              이 코드는 <strong>5분</strong> 후 만료됩니다.<br>
                              This code expires in <strong>5 minutes</strong>.
                            </p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#FAFAFA;padding:20px 32px;text-align:center;border-top:1px solid #eee;">
                            <p style="margin:0;color:#aaa;font-size:12px;">
                              본인이 요청하지 않았다면 이 이메일을 무시해주세요.<br>
                              If you did not request this, please ignore this email.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(code);
    }
}
