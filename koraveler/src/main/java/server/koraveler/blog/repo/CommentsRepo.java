package server.koraveler.blog.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.koraveler.blog.model.Comment;

import java.util.List;

@Repository
public interface CommentsRepo extends MongoRepository<Comment, String> {
    // 특정 문서의 1depth 댓글만 조회
    Page<Comment> findByDocumentIdAndDepthAndDeletedFalse(String documentId, int depth, Pageable pageable);

    // 특정 문서의 1depth 댓글만 조회 (숨김 포함 전체 - 관리자용)
    Page<Comment> findByDocumentIdAndDepth(String documentId, int depth, Pageable pageable);

    // 특정 부모 댓글의 대댓글 조회 (삭제되지 않은 것만)
    List<Comment> findByParentIdAndDeletedFalse(String parentId);

    // 특정 부모 댓글의 대댓글 조회 (전체)
    List<Comment> findByParentId(String parentId);

    // 특정 문서의 전체 댓글 수 (삭제되지 않은 것만)
    long countByDocumentIdAndDeletedFalse(String documentId);

    // 특정 문서의 전체 댓글 조회 (삭제되지 않은 것만)
    List<Comment> findByDocumentIdAndDeletedFalse(String documentId);

    // 특정 사용자가 작성한 댓글 조회
    Page<Comment> findByUserIdAndDeletedFalse(String userId, Pageable pageable);

    // depth 2인 댓글의 실제 부모(depth 1) 찾기 위한 쿼리
    // c 댓글에 대댓글 달 때, b의 부모인 a를 찾아서 b의 대댓글로 만들기 위함
    @Query("{ '_id': ?0, 'depth': 1 }")
    Comment findDepth1ParentById(String id);

    // 특정 문서의 특정 depth 댓글 수
    long countByDocumentIdAndDepthAndDeletedFalse(String documentId, int depth);
}
