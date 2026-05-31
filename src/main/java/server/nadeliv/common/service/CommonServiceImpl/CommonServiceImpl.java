package server.nadeliv.common.service.CommonServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import server.nadeliv.common.dto.OgMetadataDTO;
import server.nadeliv.common.service.CommonService;

import java.net.URI;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {
    @Value("${spring.weather.url}")
    private String weatherUrl;

    @Value("${spring.weather.key}")
    private String weatherKey;

    private static final int TIMEOUT_MS = 5000;
    // 실제 브라우저 UA. naver.me, instagram, twitter 등은 봇 UA에 og 메타가 빈 미니멀 페이지를 줌.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    @Override
    public String getWeatherData() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        String url = weatherUrl + "?appid=" + weatherKey + "&q=Seoul,kr";
        try {
            ResponseEntity<String> resData = restTemplate.getForEntity(url, String.class);
            return resData.getBody();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public OgMetadataDTO getOgMetadata(String url) throws Exception {
        // URL 유효성 검증 (SSRF 방지의 1차 방어선)
        URI uri = new URI(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("http/https URL만 허용됩니다.");
        }

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko,en-US;q=0.9,en;q=0.8")
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(false)
                .get();

        // redirect 이후 최종 URL을 base로 사용해야 상대 경로(og:image 등)가 올바르게 풀림.
        URI baseUri;
        try {
            baseUri = new URI(doc.location());
        } catch (Exception e) {
            baseUri = uri;
        }

        String title = firstNonEmpty(
                metaContent(doc, "meta[property=og:title]"),
                metaContent(doc, "meta[name=twitter:title]"),
                doc.title()
        );

        String description = firstNonEmpty(
                metaContent(doc, "meta[property=og:description]"),
                metaContent(doc, "meta[name=twitter:description]"),
                metaContent(doc, "meta[name=description]")
        );

        String image = firstNonEmpty(
                metaContent(doc, "meta[property=og:image]"),
                metaContent(doc, "meta[name=twitter:image]")
        );

        String siteName = firstNonEmpty(
                metaContent(doc, "meta[property=og:site_name]"),
                baseUri.getHost()
        );

        String favicon = firstNonEmpty(
                linkHref(doc, "link[rel~=(?i)^(shortcut icon|icon|apple-touch-icon)$]"),
                baseUri.getScheme() + "://" + baseUri.getHost() + "/favicon.ico"
        );

        // 상대 URL 보정 — redirect 후의 최종 URL 기준
        image = absolutize(image, baseUri);
        favicon = absolutize(favicon, baseUri);

        return OgMetadataDTO.builder()
                .url(url)
                .title(nullIfEmpty(title))
                .description(nullIfEmpty(description))
                .image(nullIfEmpty(image))
                .favicon(nullIfEmpty(favicon))
                .siteName(nullIfEmpty(siteName))
                .build();
    }

    private String metaContent(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el == null ? null : el.attr("content");
    }

    private String linkHref(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el == null ? null : el.attr("href");
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    private String nullIfEmpty(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private String absolutize(String maybeRelative, URI base) {
        if (maybeRelative == null || maybeRelative.isBlank()) return null;
        try {
            URI resolved = base.resolve(maybeRelative);
            return resolved.toString();
        } catch (Exception e) {
            return maybeRelative;
        }
    }
}
