package server.nadeliv.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.users.dto.CustomUserDetails;
import org.springframework.web.server.ResponseStatusException;
import server.nadeliv.blog.dto.DocumentsDTO;
import server.nadeliv.blog.dto.DocumentsInfo;
import server.nadeliv.blog.dto.FeaturedRequest;
import server.nadeliv.blog.dto.PaginationDTO;
import server.nadeliv.blog.dto.PopularPostDTO;
import server.nadeliv.blog.model.Documents;
import server.nadeliv.blog.service.BlogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
@Slf4j
public class BlogController {
    @Autowired
    private BlogService blogService;

    // 글 생성
    @PostMapping("/document")
    public ResponseEntity<?> createDocument (
            @RequestBody DocumentsDTO data
    ) {
        try {
            DocumentsDTO documentsDTO = blogService.createDocument(data);
            return ResponseEntity.ok(documentsDTO);
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/document/content")
    public ResponseEntity<?> createAfterSaveDocument (
            @RequestBody DocumentsDTO data
    ) {
        try {
            DocumentsDTO documentsDTO = blogService.createAfterSaveDocument(data);
            return ResponseEntity.ok(documentsDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/document")
    public ResponseEntity<?> saveDocument (
            @RequestBody DocumentsDTO data
    ) {
        try {
            DocumentsDTO documentsDTO = blogService.saveDocument(data);
            return ResponseEntity.ok(documentsDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/ps/documents")
    public DocumentsInfo getDocumentsByNonAuth(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("folderId") String folderId,
            @RequestParam("dateSort") String dateSort,
            @RequestParam(value = "locale", required = false) String locale
    ) {
        try {
            return blogService.getDocuments(new PaginationDTO(page, size, folderId, null, dateSort, locale));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @GetMapping("/documents")
    public DocumentsInfo getDocumentsByAuth(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("folderId") String folderId,
            @RequestParam("type") String type,
            @RequestParam("dateSort") String dateSort,
            @RequestParam(value = "locale", required = false) String locale
    ) {
        try {
            return blogService.getDocuments(new PaginationDTO(page, size, folderId, type, dateSort, locale));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @GetMapping("/ps/search/documents")
    public DocumentsInfo searchDocuments(
            @RequestParam("value") String value,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ) {
        try {
            return blogService.searchDocuments(value, new PaginationDTO(page, size, null, null, null, null));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @GetMapping("/ps/document")
    public DocumentsDTO getDocument(
        @RequestParam("id") String id
    ) {
        try {
            return blogService.getDocument(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @DeleteMapping("/ps/document")
    public ResponseEntity<?> deleteDocument(
            @RequestParam("id") String id
    ) {
        try {
            blogService.deleteDocument(id);
            Map<String, String> docId = new HashMap<>();
            docId.put("id", id);
            return ResponseEntity.ok(docId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 휴지통에서 복구 (인증 필요)
    @PatchMapping("/document/{id}/restore")
    public ResponseEntity<?> restoreDocument(@PathVariable("id") String id) {
        try {
            blogService.restoreDocument(id);
            Map<String, String> result = new HashMap<>();
            result.put("id", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Restore failed for document {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("restore failed: " + e.getMessage());
        }
    }


    /**
     * 일반 글을 Featured로 설정 (관리자용)
     */
    @PutMapping("/document/{id}/feature")
    public ResponseEntity<?> setAsFeatured(
            @PathVariable String id,
            @RequestBody FeaturedRequest request
    ) {
        try {
            String approvedBy = SecurityContextHolder.getContext()
                    .getAuthentication().getName();

            DocumentsDTO result = blogService.setAsFeatured(
                    id,
                    request.getFeaturedInfo(),
                    request.getStartDate(),
                    request.getEndDate(),
                    approvedBy
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Featured 설정 실패: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Featured 설정 실패: " + e.getMessage());
        }
    }

    /**
     * Featured 설정 해제
     */
    @DeleteMapping("/document/{id}/feature")
    public ResponseEntity<?> removeFromFeatured(@PathVariable String id) {
        try {
            DocumentsDTO result = blogService.removeFromFeatured(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Featured 해제 실패: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Featured 해제 실패: " + e.getMessage());
        }
    }

    /**
     * Featured 정보만 수정 (스케줄은 유지)
     */
    @PatchMapping("/document/{id}/feature")
    public ResponseEntity<?> updateFeaturedInfo(
            @PathVariable String id,
            @RequestBody Documents.FeaturedInfo featuredInfo
    ) {
        try {
            DocumentsDTO result = blogService.updateFeaturedInfo(id, featuredInfo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Featured 정보 수정 실패: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Featured 정보 수정 실패: " + e.getMessage());
        }
    }

    /**
     * 현재 활성화된 Featured 글 목록 (홈페이지용)
     */
    @GetMapping("/ps/featured/active")
    public ResponseEntity<?> getActiveFeaturedDocuments(
            @RequestParam(defaultValue = "3") int limit
    ) {
        try {
            List<DocumentsDTO> featuredDocs = blogService.getActiveFeaturedDocuments(limit);
            return ResponseEntity.ok(featuredDocs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Featured 문서 조회 실패");
        }
    }

    /**
     * Featured 가능한 일반 글 목록 (관리자용)
     * draft가 false이고 featured가 아닌 글들
     */
    @GetMapping("/documents/featurable")
    public ResponseEntity<?> getFeaturableDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        try {
            DocumentsInfo docs = blogService.getFeaturableDocuments(page, size, search);
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("조회 실패");
        }
    }

    /**
     * Following 피드: 내가 팔로우 중인 사용자들의 발행 글 목록 (인증 필요).
     */
    @GetMapping("/following")
    public ResponseEntity<?> getFollowingFeed(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "dateSort", required = false) String dateSort,
            @RequestParam(value = "locale", required = false) String locale,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
        }
        try {
            DocumentsInfo feed = blogService.getFollowingFeed(
                    userDetails.getUsername(),
                    new PaginationDTO(page, size, null, null, dateSort, locale)
            );
            return ResponseEntity.ok(feed);
        } catch (Exception e) {
            log.error("Following 피드 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Following 피드 조회 실패");
        }
    }

    /**
     * 인기 글 Top N (사이드바 위젯용, 비인증 허용)
     * period: day | week | month | all (기본 month)
     * limit: 1~20 (기본 3)
     */
    @GetMapping("/ps/popular")
    public ResponseEntity<?> getPopularPosts(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "3") int limit
    ) {
        try {
            List<PopularPostDTO> popular = blogService.getPopularPosts(period, limit);
            return ResponseEntity.ok(popular);
        } catch (Exception e) {
            log.error("Popular posts 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Popular posts 조회 실패");
        }
    }

    /**
     * Featured 히스토리 조회 (관리자용)
     */
    @GetMapping("/featured/history")
    public ResponseEntity<?> getFeaturedHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            DocumentsInfo history = blogService.getFeaturedHistory(page, size);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("히스토리 조회 실패");
        }
    }
}
