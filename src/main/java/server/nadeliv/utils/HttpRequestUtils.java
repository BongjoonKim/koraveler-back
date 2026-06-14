package server.nadeliv.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 요청에서 클라이언트 정보를 추출하는 유틸리티.
 * 프록시/로드밸런서(nginx, ELB) 환경을 고려해 X-Forwarded-For 등을 우선 확인한다.
 */
public final class HttpRequestUtils {

    private HttpRequestUtils() {}

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 클라이언트 IP 추출. 프록시 체인이 있으면 X-Forwarded-For의 첫 IP(원본)를 사용.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * User-Agent 추출 (없으면 "unknown").
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return (ua != null && !ua.isEmpty()) ? ua : "unknown";
    }
}
