package server.koraveler.blog.service;

import server.koraveler.blog.model.DocumentLike;

import java.util.List;
import java.util.Set;

public interface DocumentLikeService {

    // 좋아요 토글 (좋아요 없으면 추가, 있으면 삭제) - 결과: true=좋아요됨, false=취소됨
    boolean toggleLike(String documentId, String usersId);

    // 좋아요 추가
    DocumentLike addLike(String documentId, String usersId);

    // 좋아요 취소
    void removeLike(String documentId, String usersId);

    // 특정 문서에 좋아요 했는지 확인
    boolean hasLiked(String documentId, String usersId);

    // 특정 문서의 좋아요 수 조회
    long getLikeCount(String documentId);

    // 여러 문서에 대해 현재 사용자가 좋아요 했는지 확인 (문서 목록용)
    Set<String> getLikedDocumentIds(List<String> documentIds, String usersId);

    // 문서 삭제 시 해당 문서의 모든 좋아요 삭제
    void deleteAllByDocumentId(String documentId);
}