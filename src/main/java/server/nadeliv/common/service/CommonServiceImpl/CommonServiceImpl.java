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
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; NadelivBot/1.0; +https://www.koraveler.com)";

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
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(false)
                .get();

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
                uri.getHost()
        );

        String favicon = firstNonEmpty(
                linkHref(doc, "link[rel~=(?i)^(shortcut icon|icon|apple-touch-icon)$]"),
                scheme + "://" + uri.getHost() + "/favicon.ico"
        );

        // 상대 URL 보정
        image = absolutize(image, uri);
        favicon = absolutize(favicon, uri);

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
