package server.koraveler.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.service.BlogService;

import java.util.HashMap;
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
            @RequestBody DocumentsInfo.DocumentsDTO data
    ) {
        try {
            DocumentsInfo.DocumentsDTO documentsDTO = blogService.createDocument(data);
            return ResponseEntity.ok(documentsDTO);
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/document/content")
    public ResponseEntity<?> createAfterSaveDocument (
            @RequestBody DocumentsInfo.DocumentsDTO data
    ) {
        try {
            DocumentsInfo.DocumentsDTO documentsDTO = blogService.createAfterSaveDocument(data);
            return ResponseEntity.ok(documentsDTO);
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/document")
    public ResponseEntity<?> saveDocument (
            @RequestBody DocumentsInfo.DocumentsDTO data
    ) {
        try {
            DocumentsInfo.DocumentsDTO documentsDTO = blogService.saveDocument(data);
            return ResponseEntity.ok(documentsDTO);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/ps/documents")
    public DocumentsInfo getDocumentsByNonAuth(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("folderId") String folderId,
            @RequestParam("dateSort") String dateSort
    ) {
        try {
            return blogService.getDocuments(new PaginationDTO(page, size, folderId, null, dateSort));
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
            @RequestParam("dateSort") String dateSort
    ) {
        try {
            return blogService.getDocuments(new PaginationDTO(page, size, folderId, type, dateSort));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @GetMapping("/ps/search/documents")
    public DocumentsInfo searchDocuments(
            @RequestParam("value") String value,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("folderId") String folderId,
            @RequestParam("type") String type,
            @RequestParam("dateSort") String dateSort
    ) {
        try {
            return blogService.searchDocuments(value, new PaginationDTO(page, size, folderId, type, dateSort));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @GetMapping("/ps/document")
    public DocumentsInfo.DocumentsDTO getDocument(
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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
