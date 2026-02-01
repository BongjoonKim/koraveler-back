package server.koraveler.blog.service;

import org.springframework.data.domain.Pageable;
import server.koraveler.blog.dto.CommentDTO;
import server.koraveler.blog.dto.CommentPageDTO;

import java.util.List;

public interface CommentService {

    // 댓글 생성
    CommentDTO createComment(CommentDTO commentDTO, String userId) throws Exception;

    // 댓글 수정
    CommentDTO updateComment(String commentId, CommentDTO commentDTO, String userId) throws Exception;

    // 댓글 삭제 (soft delete)
    void deleteComment(String commentId, String userId) throws Exception;

    // 댓글 숨김 처리
    CommentDTO hideComment(String commentId, String userId) throws Exception;

    // 댓글 숨김 해제
    CommentDTO unhideComment(String commentId, String userId) throws Exception;

    // 특정 문서의 1depth 댓글 조회 (페이징) - 비로그인도 가능
    CommentPageDTO getRootComments(String documentId, String userId, Pageable pageable) throws Exception;

    // 특정 댓글의 대댓글 조회 - 비로그인도 가능
    List<CommentDTO> getReplies(String parentId, String userId) throws Exception;

    // 단일 댓글 조회
    CommentDTO getComment(String commentId, String userId) throws Exception;

    // 대댓글이 있는지 없는지 체크
    List<CommentDTO> getRepliesByParentId(String parentId, String userId) throws Exception;
}
