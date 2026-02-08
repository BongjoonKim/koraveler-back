package server.koraveler.blog.service;

import server.koraveler.blog.model.CommentLike;

import java.util.List;
import java.util.Set;

public interface CommentLikeService {

    // 좋아요 토글 (좋아요 없으면 추가, 있으면 삭제) - 결과: true=좋아요됨, false=취소됨
    boolean toggleLike(String commentId, String usersId);

    // 좋아요 추가
    CommentLike addLike(String commentId, String usersId);

    // 좋아요 취소
    void removeLike(String commentId, String usersId);

    // 특정 댓글에 좋아요 했는지 확인
    boolean hasLiked(String commentId, String usersId);

    // 특정 댓글의 좋아요 수 조회
    long getLikeCount(String commentId);

    // 여러 댓글에 대해 현재 사용자가 좋아요 했는지 확인 (댓글 목록용)
    Set<String> getLikedCommentIds(List<String> commentIds, String usersId);

    // 댓글 삭제 시 해당 댓글의 모든 좋아요 삭제
    void deleteAllByCommentId(String commentId);
}
