package server.nadeliv.blog.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import server.nadeliv.blog.service.SitemapService;

/**
 * 검색엔진용 sitemap.xml (비인증 공개).
 * www.nadeliv.com/robots.txt 에서 https://api.nadeliv.com/sitemap.xml 로 참조된다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SitemapController {

    private final SitemapService sitemapService;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        try {
            return ResponseEntity.ok()
                    .header("Cache-Control", "public, max-age=3600")
                    .body(sitemapService.generateSitemapXml());
        } catch (Exception e) {
            log.error("sitemap.xml 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
