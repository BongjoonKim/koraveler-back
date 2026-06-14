package server.nadeliv.users.service;

public interface SesNotificationService {

    /**
     * SNS로부터 전달된 SES 알림(원문 JSON)을 처리한다.
     * - SubscriptionConfirmation: 구독 확인(SubscribeURL 호출)
     * - Notification: Bounce(Permanent)/Complaint 발생 주소를 suppression 목록에 등록
     *
     * @param rawBody SNS가 POST한 원문 JSON 문자열
     * @return 처리 결과 요약(로깅/응답용)
     */
    String handle(String rawBody);
}
