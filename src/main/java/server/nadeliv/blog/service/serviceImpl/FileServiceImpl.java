package server.nadeliv.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.nadeliv.blog.service.FileService;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Client s3Client;

    private static final String BUCKET_NAME = "haries-img";
    private static final String REGION = "ap-northeast-2";

    // 허용 문자: 영숫자 / . _ - (경로 구분자 /). 첫 글자는 영숫자.
    private static final Pattern SAFE_KEY = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]*$");

    @Override
    public String uploadBlogFile(MultipartFile file, String fileKey) {
        validateFileKey(fileKey);
        validateMediaType(file);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = "https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com/" + fileKey;
            log.info("Blog S3 upload success: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("Blog S3 upload failed: {}", e.getMessage());
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED, e.getMessage());
        }
    }

    /**
     * path traversal · 절대경로 · 임의 버킷 지정 차단.
     * 인증된 사용자라도 의도하지 않은 위치에 쓰지 못하도록 key 형식을 엄격히 제한한다.
     */
    private void validateFileKey(String fileKey) {
        if (fileKey == null
                || fileKey.isBlank()
                || fileKey.startsWith("/")
                || fileKey.contains("..")
                || fileKey.contains("\\")
                || fileKey.contains("://")
                || !SAFE_KEY.matcher(fileKey).matches()) {
            throw new CustomException(ErrorCode.INVALID_FILE_KEY, fileKey);
        }
    }

    private void validateMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new CustomException(ErrorCode.INVALID_MEDIA_TYPE);
        }
    }
}
