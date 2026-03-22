package server.nadeliv.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.blog.dto.DocumentLikeDTO;
import server.nadeliv.blog.service.DocumentLikeService;
import server.nadeliv.users.dto.CustomUserDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentLikeController {

    private final DocumentLikeService documentLikeService;

    /**
     * 좋아요 토글 (로그인 필수)
     * POST /api/v1/documents/{documentId}/like
     */
    @PostMapping("/{documentId}/like")
    public ResponseEntity<DocumentLikeDTO> toggleLike(
            @PathVariable String documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String usersId = userDetails.getUsername();
        boolean isLiked = documentLikeService.toggleLike(documentId, usersId);
        long likeCount = documentLikeService.getLikeCount(documentId);

        DocumentLikeDTO response = DocumentLikeDTO.builder()
                .documentId(documentId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 문서의 좋아요 상태 조회 (비로그인 허용)
     * GET /api/v1/documents/ps/{documentId}/like
     */
    @GetMapping("/ps/{documentId}/like")
    public ResponseEntity<DocumentLikeDTO> getLikeStatus(
            @PathVariable String documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean isLiked = false;
        if (userDetails != null) {
            isLiked = documentLikeService.hasLiked(documentId, userDetails.getUsername());
        }
        long likeCount = documentLikeService.getLikeCount(documentId);

        DocumentLikeDTO response = DocumentLikeDTO.builder()
                .documentId(documentId)
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 여러 문서의 좋아요 상태 일괄 조회 (문서 목록용, 비로그인 허용)
     * POST /api/v1/documents/ps/likes/status
     */
    @PostMapping("/ps/likes/status")
    public ResponseEntity<Map<String, Object>> getLikeStatuses(
            @RequestBody Map<String, List<String>> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<String> documentIds = request.get("documentIds");

        if (documentIds == null || documentIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("likedDocumentIds", Set.of()));
        }

        Set<String> likedDocumentIds = Set.of();
        if (userDetails != null) {
            likedDocumentIds = documentLikeService.getLikedDocumentIds(documentIds, userDetails.getUsername());
        }

        return ResponseEntity.ok(Map.of("likedDocumentIds", likedDocumentIds));
    }
}