package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageReactionRequest {
    @NotBlank(message = "이모지는 필수입니다")
    private String emoji;

    private Boolean isAdd; // true: 추가, false: 제거
}