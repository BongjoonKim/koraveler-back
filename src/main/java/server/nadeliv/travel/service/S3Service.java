package server.nadeliv.travel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
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

    /**
     * S3 URL에서 버킷명과 key를 모두 추출해 삭제.
     * BUCKET_NAME 하드코딩에 의존하지 않으므로 haries-img/haries-thumbnail 등
     * 임의의 버킷에 있는 객체를 삭제할 때 사용.
     */
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            S3UrlParts parts = parseS3Url(fileUrl);
            if (parts == null) {
                log.warn("S3 URL parse failed, skipping delete: {}", fileUrl);
                return;
            }
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(parts.bucket)
                    .key(parts.key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 delete success: bucket={}, key={}", parts.bucket, parts.key);
        } catch (Exception e) {
            log.error("S3 deleteByUrl failed for {}: {}", fileUrl, e.getMessage());
        }
    }

    private record S3UrlParts(String bucket, String key) {}

    /**
     * 지원 형식:
     *  - https://{bucket}.s3.{region}.amazonaws.com/{key}
     *  - https://{bucket}.s3.amazonaws.com/{key}
     *  - https://s3.{region}.amazonaws.com/{bucket}/{key}
     */
    private S3UrlParts parseS3Url(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath() != null ? uri.getPath() : "";
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (host == null) return null;

            // virtual-hosted style: {bucket}.s3...
            int s3Idx = host.indexOf(".s3");
            if (s3Idx > 0) {
                String bucket = host.substring(0, s3Idx);
                if (path.isEmpty()) return null;
                return new S3UrlParts(bucket, path);
            }
            // path-style: s3.region.amazonaws.com/{bucket}/{key}
            if (host.startsWith("s3")) {
                int slash = path.indexOf('/');
                if (slash <= 0) return null;
                String bucket = path.substring(0, slash);
                String key = path.substring(slash + 1);
                return new S3UrlParts(bucket, key);
            }
            return null;
        } catch (Exception e) {
            return null;
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
