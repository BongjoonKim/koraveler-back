package server.koraveler.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageUpdateRequest {
    @NotBlank(message = "수정할 메시지 내용은 필수입니다")
    @Size(max = 2000, message = "메시지는 2000자를 초과할 수 없습니다")
    private String message;
}