package server.nadeliv.i18n.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * Locale 감지 체인:
 * 1. ?locale= 쿼리 파라미터
 * 2. 쿠키 preferred_locale
 * 3. Accept-Language 헤더
 * 4. 원본 언어 fallback (null 반환)
 */
@Component
public class I18nLocaleDetector {

    private static final Set<String> SUPPORTED_LOCALES = Set.of("ko", "en", "zh", "ja");

    /**
     * 요청에서 locale을 감지. null이면 원본 언어 사용.
     */
    public String resolve(HttpServletRequest request, String queryParam) {
        // 1. 쿼리 파라미터
        if (queryParam != null && !"original".equals(queryParam) && isSupported(queryParam)) {
            return queryParam;
        }
        if ("original".equals(queryParam)) {
            return null;
        }

        // 2. 쿠키
        Cookie cookie = WebUtils.getCookie(request, "preferred_locale");
        if (cookie != null && isSupported(cookie.getValue())) {
            return cookie.getValue();
        }

        // 3. Accept-Language 헤더
        String acceptLang = parseAcceptLanguage(request.getHeader("Accept-Language"));
        if (acceptLang != null && isSupported(acceptLang)) {
            return acceptLang;
        }

        // 4. Fallback → null (원본 사용)
        return null;
    }

    private boolean isSupported(String code) {
        return code != null && SUPPORTED_LOCALES.contains(code.toLowerCase());
    }

    private String parseAcceptLanguage(String header) {
        if (header == null) return null;
        // "ko-KR,ko;q=0.9,en;q=0.8" → "ko"
        return Arrays.stream(header.split(","))
                .map(s -> s.split(";")[0].trim().split("-")[0].toLowerCase())
                .filter(this::isSupported)
                .findFirst()
                .orElse(null);
    }
}
