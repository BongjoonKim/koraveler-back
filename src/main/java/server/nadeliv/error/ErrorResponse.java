package server.nadeliv.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    // 기존 필드 유지
    private HttpStatus status;
    private String msg;

    // 추가 필드 (선택적)
    private String code;        // 에러 코드 (예: TRANS_001)
    private String detail;      // 상세 에러 메시지
    private String path;        // 요청 경로
    private LocalDateTime timestamp;  // 에러 발생 시간

    // 기존 생성자와의 호환성을 위한 정적 팩토리 메소드
    public static ErrorResponse of(HttpStatus status, String msg) {
        return ErrorResponse.builder()
                .status(status)
                .msg(msg)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ErrorCode를 사용한 정적 팩토리 메소드
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .msg(errorCode.getMsg())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ErrorCode와 상세 메시지를 사용한 정적 팩토리 메소드
    public static ErrorResponse of(ErrorCode errorCode, String detail) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .msg(errorCode.getMsg())
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // CustomException으로부터 생성
    public static ErrorResponse from(CustomException e) {
        return ErrorResponse.builder()
                .status(e.getStatus())
                .code(e.getCode())
                .msg(e.getMsg())
                .detail(e.getDetail())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // HTTP 상태 코드 숫자값 반환 (JSON 응답용)
    @JsonProperty("statusCode")
    public int getStatusCode() {
        return status != null ? status.value() : 0;
    }

    // HTTP 상태 이름 반환 (JSON 응답용)
    @JsonProperty("statusName")
    public String getStatusName() {
        return status != null ? status.name() : null;
    }
}