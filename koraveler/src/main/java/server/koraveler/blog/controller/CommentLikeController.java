package server.koraveler.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.blog.dto.CommentLikeDTO;
import server.koraveler.blog.service.CommentLikeService;
import server.koraveler.users.dto.CustomUserDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    // 좋아요 토글
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeDTO> toggleLike(
            @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String usersId = userDetails.getUsername();
        boolean isLiked = commentLikeService.toggleLike(commentId, usersId);
        long likeCount = commentLikeService.getLikeCount(commentId);

        CommentLikeDTO response = CommentLikeDTO.builder()
                .commentId(commentId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();

        return ResponseEntity.ok(response);
    }

    // 특정 댓글의 좋아요 상태 조회
    @GetMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeDTO> getLikeStatus(
            @PathVariable String commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean isLiked = false;
        if (userDetails != null) {
            isLiked = commentLikeService.hasLiked(commentId, userDetails.getUsername());
        }
        long likeCount = commentLikeService.getLikeCount(commentId);

        CommentLikeDTO response = CommentLikeDTO.builder()
                .commentId(commentId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();

        return ResponseEntity.ok(response);
    }

    // 여러 댓글의 좋아요 상태 일괄 조회 (댓글 목록용)
    @PostMapping("/likes/status")
    public ResponseEntity<Map<String, Object>> getLikeStatuses(
            @RequestBody Map<String, List<String>> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<String> commentIds = request.get("commentIds");

        if (commentIds == null || commentIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("likedCommentIds", Set.of()));
        }

        Set<String> likedCommentIds = Set.of();
        if (userDetails != null) {
            likedCommentIds = commentLikeService.getLikedCommentIds(commentIds, userDetails.getUsername());
        }

        return ResponseEntity.ok(Map.of("likedCommentIds", likedCommentIds));
    }
}
