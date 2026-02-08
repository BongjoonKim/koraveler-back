package server.koraveler.connections.bookmarks.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import server.koraveler.connections.bookmarks.dto.BookmarkDTO;
import server.koraveler.connections.bookmarks.model.Bookmark;
import server.koraveler.connections.bookmarks.service.ConBookmarksUsersDocumentsService;

@RestController
@RequestMapping("bookmark")
@RequiredArgsConstructor
@Slf4j
public class ConBookmarksUsersDocumentsController {
    @Autowired
    private ConBookmarksUsersDocumentsService bookmarkService;

    // 글에 북마크 생성
    @PostMapping("")
    public ResponseEntity<?> createBookmark(
            @RequestBody BookmarkDTO bookmarkDTO
    ) {
        try {
            return ResponseEntity.ok(bookmarkService.createBookmark(bookmarkDTO));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    // 글에 북마크 여부 확인
    @GetMapping("/document")
    public ResponseEntity<?> getBookmark(
            @RequestParam("id") String documentId
    ) {
        try {
            return ResponseEntity.ok(bookmarkService.isBookmarked(documentId));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    // 글에 북마크 삭제
    @DeleteMapping("/document")
    public ResponseEntity<?> deleteBookmark(
            @RequestParam("id") String documentId
    ) {
        try {
            bookmarkService.deleteBookmarkByDocumentsId(documentId);
            return ResponseEntity.ok(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }
}
