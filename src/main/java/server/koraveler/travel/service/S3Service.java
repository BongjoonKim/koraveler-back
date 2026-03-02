package server.koraveler.travel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.koraveler.error.CustomException;
import server.koraveler.error.ErrorCode;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    private static final String BUCKET_NAME = "koraveler-travel";
    private static final String BASE_PATH = "travel-projects";

    public String uploadFile(MultipartFile file, String travelId) {
        validateMediaType(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String fileName = UUID.randomUUID() + extension;
        String key = BASE_PATH + "/" + travelId + "/origin/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/" + key;
            log.info("S3 upload success: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("S3 upload failed: {}", e.getMessage());
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED, e.getMessage());
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("S3 download failed: {}", e.getMessage());
            throw new CustomException(ErrorCode.S3_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 delete success: {}", key);
        } catch (Exception e) {
            log.error("S3 delete failed: {}", e.getMessage());
        }
    }

    private void validateMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new CustomException(ErrorCode.INVALID_MEDIA_TYPE);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private static final List<String> VIDEO_EXTENSIONS = List.of(
            ".mp4", ".webm", ".mov", ".avi", ".mkv", ".quicktime"
    );

    /**
     * origin 파일 URL로부터 썸네일 URL을 생성
     * Lambda 규칙: /origin/ → /thumbnails/
     * - 이미지: 확장자 유지 (photo.jpg → photo.jpg)
     * - 비디오: 확장자 .jpg로 변경 (video.mov → video.jpg)
     */
    public String buildThumbnailUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/origin/")) {
            return null;
        }
        String thumbnailUrl = fileUrl.replace("/origin/", "/thumbnails/");
        if (isVideoFile(fileUrl)) {
            int lastDotIndex = thumbnailUrl.lastIndexOf(".");
            if (lastDotIndex > 0) {
                thumbnailUrl = thumbnailUrl.substring(0, lastDotIndex) + ".jpg";
            }
        }
        return thumbnailUrl;
    }

    private boolean isVideoFile(String url) {
        String lowerUrl = url.toLowerCase();
        return VIDEO_EXTENSIONS.stream().anyMatch(lowerUrl::endsWith);
    }

    private String extractKeyFromUrl(String fileUrl) {
        String prefix = "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        return fileUrl;
    }
}
