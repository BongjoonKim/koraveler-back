// 7. AttachmentRequest.java
package server.koraveler.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttachmentRequest {
    @NotBlank(message = "파일명은 필수입니다")
    private String fileName;

    @NotBlank(message = "파일 URL은 필수입니다")
    private String fileUrl;

    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer duration;
}