package server.koraveler.folders.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.koraveler.blog.service.BlogService;
import server.koraveler.folders.dto.FoldersDTO;
import server.koraveler.folders.service.FolderService;

@RestController
@RequestMapping("ps/folders")
@RequiredArgsConstructor
@Slf4j
public class FoldersController {
    @Autowired
    private FolderService folderService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllLoginUserFolders() {
        try {
            return ResponseEntity.ok(folderService.getAllLoginUserFolders());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }

    @PostMapping("")
    public ResponseEntity<?> createFolder(
        @RequestBody FoldersDTO foldersDTO
    ) {
        try {
            return ResponseEntity.ok(folderService.createFolder(foldersDTO));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }

    @PutMapping("")
    public ResponseEntity<?> saveFolder(
            @RequestBody FoldersDTO foldersDTO
    ) {
        try {
            return ResponseEntity.ok(folderService.saveFolder(foldersDTO));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<?> deleteFolder(
            @RequestParam("id") String folderId
    ) {
        try {
            folderService.deleteFolder(folderId);
            return ResponseEntity.ok(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }
}
