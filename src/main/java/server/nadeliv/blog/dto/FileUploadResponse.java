package server.nadeliv.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    // 업로드된 파일의 공개 S3 URL (에디터 <img src>/<video src>에 그대로 사용)
    private String url;
}
