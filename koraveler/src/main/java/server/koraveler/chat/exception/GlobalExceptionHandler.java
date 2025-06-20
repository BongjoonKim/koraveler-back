// GlobalExceptionHandler.java
package server.koraveler.chat.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "server.koraveler.chat")
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("Custom exception occurred: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(e.getErrorCode().getCode())
                .message(e.getErrorCode().getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        HttpStatus status = getHttpStatusFromErrorCode(e.getErrorCode());

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation exception occurred: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("입력값 검증에 실패했습니다")
                .details(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument exception occurred: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INVALID_ARGUMENT")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected exception occurred: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus getHttpStatusFromErrorCode(ErrorCode errorCode) {
        switch (errorCode.getCode().substring(0, 3)) {
            case "MSG":
                if (errorCode == ErrorCode.MESSAGE_NOT_FOUND) {
                    return HttpStatus.NOT_FOUND;
                }
                return HttpStatus.FORBIDDEN;
            case "CHN":
                if (errorCode == ErrorCode.CHANNEL_NOT_FOUND) {
                    return HttpStatus.NOT_FOUND;
                }
                if (errorCode == ErrorCode.DUPLICATE_CHANNEL_NAME) {
                    return HttpStatus.CONFLICT;
                }
                if (errorCode == ErrorCode.CHANNEL_FULL ||
                        errorCode == ErrorCode.INVALID_CHANNEL_PASSWORD ||
                        errorCode == ErrorCode.CHANNEL_APPROVAL_REQUIRED) {
                    return HttpStatus.BAD_REQUEST;
                }
                return HttpStatus.FORBIDDEN;
            case "MBR":
                if (errorCode == ErrorCode.NOT_CHANNEL_MEMBER) {
                    return HttpStatus.NOT_FOUND;
                }
                if (errorCode == ErrorCode.ALREADY_CHANNEL_MEMBER) {
                    return HttpStatus.CONFLICT;
                }
                return HttpStatus.FORBIDDEN;
            case "GEN":
                if (errorCode == ErrorCode.INVALID_INPUT) {
                    return HttpStatus.BAD_REQUEST;
                }
                return HttpStatus.INTERNAL_SERVER_ERROR;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}