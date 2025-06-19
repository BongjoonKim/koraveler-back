// 6. AttachmentResponse.java
package server.koraveler.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttachmentResponse {
    private String id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
    private String formattedFileSize; // "1.2MB" 형태
    private Integer width;
    private Integer height;
    private Integer duration;
    private String thumbnailUrl; // 썸네일 URL
}