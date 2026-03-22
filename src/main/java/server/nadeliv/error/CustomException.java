package server.nadeliv.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String msg;
    private final String detail;

    // ErrorCode를 사용하는 생성자 (권장)
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.status = errorCode.getStatus();
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
        this.detail = null;
    }

    // ErrorCode와 추가 상세 메시지를 사용하는 생성자
    public CustomException(ErrorCode errorCode, String detail) {
        super(errorCode.getMsg() + ": " + detail);
        this.status = errorCode.getStatus();
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
        this.detail = detail;
    }

    // 기존 생성자 유지 (하위 호환성)
    public CustomException(HttpStatus status, String msg) {
        super(msg);
        this.status = status;
        this.code = null;
        this.msg = msg;
        this.detail = null;
    }

    // 전체 정보를 커스텀하게 설정하는 생성자
    public CustomException(HttpStatus status, String code, String msg, String detail) {
        super(msg + (detail != null ? ": " + detail : ""));
        this.status = status;
        this.code = code;
        this.msg = msg;
        this.detail = detail;
    }

    // 클라이언트에 전달할 에러 응답 생성
    public ErrorResponse toErrorResponse() {
        return ErrorResponse.from(this);
    }

    // 경로 정보를 포함한 에러 응답 생성
    public ErrorResponse toErrorResponse(String path) {
        ErrorResponse response = ErrorResponse.from(this);
        response.setPath(path);
        return response;
    }
}