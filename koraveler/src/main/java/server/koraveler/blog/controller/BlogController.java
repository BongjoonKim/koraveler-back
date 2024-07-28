package server.koraveler.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.service.BlogService;

import java.util.HashMap;

@RestController
@RequestMapping("blog")
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
}
