package server.nadeliv.blog.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 블로그/댓글 에디터 이미지·비디오를 S3(haries-img)에 업로드하고 공개 URL을 반환한다.
     * 프론트가 더 이상 AWS 키로 브라우저에서 직접 업로드하지 않고 이 엔드포인트를 경유한다.
     *
     * @param file    업로드할 파일 (image/* 또는 video/* 만 허용)
     * @param fileKey S3 object key (예: new/{uuid}, {documentId}/{uuid}, comments/{documentId}/{uuid})
     * @return 업로드된 객체의 공개 URL
     */
    String uploadBlogFile(MultipartFile file, String fileKey);
}
