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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 검색엔진(Google Search Console, Naver Search Advisor)에 제출할 sitemap.xml 생성.
 * 발행된(draft 아님, 삭제 아님, 공개) 글의 원본 locale URL + 번역 완료된 locale URL 을 포함하며,
 * 언어 버전 간 관계를 알리기 위해 xhtml:link rel="alternate" hreflang 을 함께 기재한다.
 * (hreflang 없이 언어별 URL만 나열하면 검색 결과에 언어가 섞여 노출된다)
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
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"")
                .append(" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">\n");

        // 홈 (단일 URL, 언어 버전 없음)
        appendUrl(xml, baseUrl + "/", null, null);

        // locale 별 블로그 목록: 서로가 언어 대체 버전 (x-default = ko)
        Map<String, String> homeAlternates = new LinkedHashMap<>();
        homeAlternates.put("ko", baseUrl + "/blog/home/ko");
        for (String locale : targetLocales.split(",")) {
            String trimmed = locale.trim();
            homeAlternates.put(trimmed, baseUrl + "/blog/home/" + trimmed);
        }
        for (String url : homeAlternates.values()) {
            appendUrl(xml, url, null, buildAlternates(homeAlternates, homeAlternates.get("ko")));
        }

        // 발행 글 + 번역본: 글 단위로 언어 그룹을 만들어 hreflang 상호 참조
        List<Documents> publishedDocs = blogsRepo.findAllForSitemap();
        List<PostTranslation> translations = postTranslationRepo.findByStatusIn(
                List.of(TranslationStatus.COMPLETED, TranslationStatus.MANUALLY_EDITED));

        Map<String, List<PostTranslation>> translationsByPost = new LinkedHashMap<>();
        for (PostTranslation t : translations) {
            translationsByPost.computeIfAbsent(t.getPostId(), k -> new ArrayList<>()).add(t);
        }

        for (Documents doc : publishedDocs) {
            String originalLocale = doc.getOriginalLocale() != null ? doc.getOriginalLocale() : "ko";
            String originalUrl = baseUrl + "/blog/view/" + originalLocale + "/" + doc.getId();

            // 언어 그룹: 원본 + 완료된 번역
            Map<String, String> group = new LinkedHashMap<>();
            Map<String, LocalDateTime> lastmods = new LinkedHashMap<>();
            group.put(originalLocale, originalUrl);
            lastmods.put(originalLocale, doc.getUpdated() != null ? doc.getUpdated() : doc.getCreated());

            for (PostTranslation t : translationsByPost.getOrDefault(doc.getId(), List.of())) {
                group.put(t.getLocale(), baseUrl + "/blog/view/" + t.getLocale() + "/" + doc.getId());
                lastmods.put(t.getLocale(), t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt());
            }

            String alternates = buildAlternates(group, originalUrl);
            for (Map.Entry<String, String> entry : group.entrySet()) {
                appendUrl(xml, entry.getValue(), lastmods.get(entry.getKey()), alternates);
            }
        }

        xml.append("</urlset>\n");

        String result = xml.toString();
        cachedXml = result;
        cachedAt = System.currentTimeMillis();
        log.info("sitemap.xml 생성 완료: 발행 글 {}건, 번역 {}건", publishedDocs.size(), translations.size());
        return result;
    }

    // 언어 그룹 전체의 xhtml:link alternate 블록 생성 (그룹 내 모든 URL 이 동일 블록을 공유)
    private String buildAlternates(Map<String, String> group, String xDefaultUrl) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : group.entrySet()) {
            sb.append("    <xhtml:link rel=\"alternate\" hreflang=\"").append(entry.getKey())
                    .append("\" href=\"").append(escapeXml(entry.getValue())).append("\"/>\n");
        }
        sb.append("    <xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"")
                .append(escapeXml(xDefaultUrl)).append("\"/>\n");
        return sb.toString();
    }

    private void appendUrl(StringBuilder xml, String loc, LocalDateTime lastmod, String alternates) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod.format(LASTMOD_FORMAT)).append("</lastmod>\n");
        }
        if (alternates != null) {
            xml.append(alternates);
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
