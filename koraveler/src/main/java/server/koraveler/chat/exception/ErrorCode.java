package server.koraveler.chat.exception;
import lombok.Getter;

@Getter
public enum ErrorCode {
    // Message 관련 에러
    MESSAGE_NOT_FOUND("MSG001", "메시지를 찾을 수 없습니다"),
    UNAUTHORIZED_MESSAGE_ACCESS("MSG002", "메시지 접근 권한이 없습니다"),
    UNAUTHORIZED_MESSAGE_DELETE("MSG003", "메시지 삭제 권한이 없습니다"),
    UNAUTHORIZED_PIN_MESSAGE("MSG004", "메시지 고정 권한이 없습니다"),

    // Channel 관련 에러
    CHANNEL_NOT_FOUND("CHN001", "채널을 찾을 수 없습니다"),
    DUPLICATE_CHANNEL_NAME("CHN002", "이미 존재하는 채널명입니다"),
    UNAUTHORIZED_CHANNEL_ACCESS("CHN003", "채널 접근 권한이 없습니다"),
    UNAUTHORIZED_CHANNEL_UPDATE("CHN004", "채널 수정 권한이 없습니다"),
    UNAUTHORIZED_CHANNEL_DELETE("CHN005", "채널 삭제 권한이 없습니다"),
    ARCHIVED_CHANNEL("CHN006", "아카이브된 채널입니다"),
    CHANNEL_FULL("CHN007", "채널 최대 인원을 초과했습니다"),
    INVALID_CHANNEL_PASSWORD("CHN008", "채널 비밀번호가 잘못되었습니다"),
    CHANNEL_APPROVAL_REQUIRED("CHN009", "채널 참여 승인이 필요합니다"),

    // Member 관련 에러
    NOT_CHANNEL_MEMBER("MBR001", "채널 멤버가 아닙니다"),
    ALREADY_CHANNEL_MEMBER("MBR002", "이미 채널 멤버입니다"),
    UNAUTHORIZED_ADD_MEMBER("MBR003", "멤버 추가 권한이 없습니다"),
    UNAUTHORIZED_REMOVE_MEMBER("MBR004", "멤버 제거 권한이 없습니다"),
    UNAUTHORIZED_UPDATE_MEMBER("MBR005", "멤버 정보 수정 권한이 없습니다"),
    UNAUTHORIZED_UPDATE_ROLE("MBR006", "역할 변경 권한이 없습니다"),
    UNAUTHORIZED_MUTE_MEMBER("MBR007", "멤버 음소거 권한이 없습니다"),
    UNAUTHORIZED_INVITE("MBR008", "초대 권한이 없습니다"),

    // 일반 에러
    INVALID_INPUT("GEN001", "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR("GEN002", "서버 내부 오류가 발생했습니다");

    private final String code;
    private final String message;

    // Enum 생성자
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}