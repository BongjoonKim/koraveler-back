package server.nadeliv.blog.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.blog.model.CommentLike;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikesRepo extends MongoRepository<CommentLike, String> {

    // 특정 댓글에 특정 사용자가 좋아요 했는지 확인
    Optional<CommentLike> findByCommentIdAndUsersId(String commentId, String usersId);

    // 특정 댓글에 특정 사용자가 좋아요 했는지 여부
    boolean existsByCommentIdAndUsersId(String commentId, String usersId);

    // 특정 댓글의 좋아요 삭제 (좋아요 취소)
    void deleteByCommentIdAndUsersId(String commentId, String usersId);

    // 특정 댓글의 좋아요 수 조회
    long countByCommentId(String commentId);

    // 특정 댓글의 모든 좋아요 조회 (좋아요한 사람 목록)
    List<CommentLike> findByCommentId(String commentId);

    // 특정 사용자가 좋아요한 모든 댓글 좋아요 조회
    List<CommentLike> findByUsersId(String usersId);

    // 여러 댓글에 대해 특정 사용자가 좋아요 했는지 확인 (댓글 목록 조회 시 사용)
    List<CommentLike> findByCommentIdInAndUsersId(List<String> commentIds, String usersId);

    // 댓글 삭제 시 해당 댓글의 모든 좋아요 삭제
    void deleteByCommentId(String commentId);
}
