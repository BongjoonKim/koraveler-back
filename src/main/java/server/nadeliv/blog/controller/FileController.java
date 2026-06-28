package server.nadeliv.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.nadeliv.blog.dto.FileUploadResponse;
import server.nadeliv.blog.service.FileService;
import server.nadeliv.users.dto.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    /**
     * 에디터 이미지·비디오 업로드 (인증 필요).
     * 프론트는 multipart/form-data 로 file + fileKey 를 전송하고, 공개 S3 URL 을 돌려받는다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("fileKey") String fileKey,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String url = fileService.uploadBlogFile(file, fileKey);
            return ResponseEntity.ok(FileUploadResponse.builder().url(url).build());
        } catch (Exception e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("파일 업로드 실패: " + e.getMessage());
        }
    }
}
