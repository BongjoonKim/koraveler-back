package server.koraveler.blog.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.blog.dto.CommentDTO;
import server.koraveler.blog.dto.CommentPageDTO;
import server.koraveler.blog.service.CommentService;
import server.koraveler.users.dto.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;

//    댓글 생성
    @PostMapping("")
    public ResponseEntity<?> createComment(
            @RequestBody CommentDTO commentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try{
            String userId = userDetails.getUsername();
            CommentDTO createdComment = commentService.createComment(commentDTO, userId);
            return ResponseEntity.ok(createdComment);
        } catch (Exception e) {
        log.error("댓글 생성 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("댓글 생성 실패: " + e.getMessage());
        }
    }

//    댓글 수정 (로그인 필수 + 본인 comment만)
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable String commentId,
            @RequestBody CommentDTO commentDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            CommentDTO updatedComment = commentService.updateComment(commentId, commentDTO, userId);
            return ResponseEntity.ok(updatedComment);
        } catch (Exception e) {
            log.error("댓글 수정 실패: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("댓글 수정 실패: " + e.getMessage());
        }
    }

    /**
     * 댓글 삭제 (로그인 필수, 작성자만) - soft delete
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.ok().body("댓글이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("댓글 삭제 실패: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("댓글 삭제 실패: " + e.getMessage());
        }
    }

    /**
     * 댓글 숨김 처리 (로그인 필수, 작성자만)
     */
    @PatchMapping("/{commentId}/hide")
    public ResponseEntity<?> hideComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            CommentDTO hiddenComment = commentService.hideComment(commentId, userId);
            return ResponseEntity.ok(hiddenComment);
        } catch (Exception e) {
            log.error("댓글 숨김 처리 실패: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("댓글 숨김 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 댓글 숨김 해제 (로그인 필수, 작성자만)
     */
    @PatchMapping("/{commentId}/unhide")
    public ResponseEntity<?> unhideComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = userDetails.getUsername();
            CommentDTO unhiddenComment = commentService.unhideComment(commentId, userId);
            return ResponseEntity.ok(unhiddenComment);
        } catch (Exception e) {
            log.error("댓글 숨김 해제 실패: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("댓글 숨김 해제 실패: " + e.getMessage());
        }
    }

    /**
     * 특정 문서의 1depth 댓글 조회 (비로그인 허용)
     */
    @GetMapping("/ps/document/{documentId}")
    public ResponseEntity<?> getRootComments(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            // 비로그인 사용자는 userDetails가 null
            String userId = (userDetails != null) ? userDetails.getUsername() : null;

            Sort sortOrder = "asc".equalsIgnoreCase(sort)
                    ? Sort.by("created").ascending()
                    : Sort.by("created").descending();
            Pageable pageable = PageRequest.of(page, size, sortOrder);

            CommentPageDTO comments = commentService.getRootComments(documentId, userId, pageable);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("댓글 조회 실패: documentId={}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("댓글 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 특정 댓글의 대댓글 조회 (비로그인 허용)
     */
    @GetMapping("/ps/{parentId}/replies")
    public ResponseEntity<?> getReplies(
            @PathVariable String parentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = (userDetails != null) ? userDetails.getUsername() : null;

            List<CommentDTO> replies = commentService.getReplies(parentId, userId);
            return ResponseEntity.ok(replies);
        } catch (Exception e) {
            log.error("대댓글 조회 실패: parentId={}", parentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("대댓글 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 단일 댓글 조회 (비로그인 허용)
     */
    @GetMapping("/ps/{commentId}")
    public ResponseEntity<?> getComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String userId = (userDetails != null) ? userDetails.getUsername() : null;

            CommentDTO comment = commentService.getComment(commentId, userId);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("댓글 조회 실패: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("댓글 조회 실패: " + e.getMessage());
        }
    }
}
