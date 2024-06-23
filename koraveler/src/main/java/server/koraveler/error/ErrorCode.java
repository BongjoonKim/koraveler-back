package server.koraveler.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@NoArgsConstructor
// 프론트 에러코드와 동기화 필요
public enum ErrorCode {
    ACCESSTOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AccessToken expired"),

    REFESHTOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,"RefreshToken expired"),
    ACCESSTOKEN_NULL(HttpStatus.NOT_FOUND, "AccessToken is null");

    private HttpStatus status;
    private String msg;

}
