package server.nadeliv.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.nadeliv.blog.model.Documents;
import server.nadeliv.blog.repo.BlogsRepo;
import server.nadeliv.blog.service.SitemapService;
import server.nadeliv.i18n.model.entities.PostTranslation;
import server.nadeliv.i18n.model.enums.TranslationStatus;
import server.nadeliv.i18n.repo.PostTranslationRepo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 검색엔진(Google Search Console, Naver Search Advisor)에 제출할 sitemap.xml 생성.
 * 발행된(draft 아님, 삭제 아님, 공개) 글의 원본 locale URL + 번역 완료된 locale URL 을 포함한다.
 * www.nadeliv.com/robots.txt 의 Sitemap 지시자가 이 엔드포인트(api.nadeliv.com/sitemap.xml)를 참조한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SitemapServiceImpl implements SitemapService {

    private final BlogsRepo blogsRepo;
    private final PostTranslationRepo postTranslationRepo;

    @Value("${nadeliv.sitemap.base-url:https://www.nadeliv.com}")
    private String baseUrl;

    @Value("${nadeliv.i18n.target-locales:en,zh,ja}")
    private String targetLocales;

    private static final DateTimeFormatter LASTMOD_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L; // 10분

    // 트래픽 대비 단순 인메모리 캐시 (문서 수가 적어 Redis 까지는 불필요)
    private volatile String cachedXml;
    private volatile long cachedAt;

    @Override
    public String generateSitemapXml() {
        String cached = cachedXml;
        if (cached != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MILLIS) {
            return cached;
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 정적 페이지: 홈 + locale 별 블로그 목록
        appendUrl(xml, baseUrl + "/", null);
        appendUrl(xml, baseUrl + "/blog/home/ko", null);
        for (String locale : targetLocales.split(",")) {
            appendUrl(xml, baseUrl + "/blog/home/" + locale.trim(), null);
        }

        // 발행 글: 원본 locale URL
        List<Documents> publishedDocs = blogsRepo.findAllForSitemap();
        Set<String> publishedIds = publishedDocs.stream()
                .map(Documents::getId)
                .collect(Collectors.toSet());

        for (Documents doc : publishedDocs) {
            String locale = doc.getOriginalLocale() != null ? doc.getOriginalLocale() : "ko";
            appendUrl(xml, baseUrl + "/blog/view/" + locale + "/" + doc.getId(),
                    doc.getUpdated() != null ? doc.getUpdated() : doc.getCreated());
        }

        // 번역 완료된 글: locale 별 URL (발행 글에 한함)
        List<PostTranslation> translations = postTranslationRepo.findByStatus(TranslationStatus.COMPLETED);
        for (PostTranslation translation : translations) {
            if (!publishedIds.contains(translation.getPostId())) {
                continue;
            }
            appendUrl(xml, baseUrl + "/blog/view/" + translation.getLocale() + "/" + translation.getPostId(),
                    translation.getUpdatedAt() != null ? translation.getUpdatedAt() : translation.getCreatedAt());
        }

        xml.append("</urlset>\n");

        String result = xml.toString();
        cachedXml = result;
        cachedAt = System.currentTimeMillis();
        log.info("sitemap.xml 생성 완료: 발행 글 {}건, 번역 {}건", publishedDocs.size(), translations.size());
        return result;
    }

    private void appendUrl(StringBuilder xml, String loc, LocalDateTime lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod.format(LASTMOD_FORMAT)).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
