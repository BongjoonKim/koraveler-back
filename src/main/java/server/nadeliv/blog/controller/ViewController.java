package server.nadeliv.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.blog.dto.DocumentViewResponse;
import server.nadeliv.blog.dto.IncreaseViewRequest;
import server.nadeliv.blog.dto.ViewDTO;
import server.nadeliv.blog.dto.ViewStatsDTO;
import server.nadeliv.blog.service.ViewService;
import server.nadeliv.blog.service.serviceImpl.ViewServiceImpl;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/views")
@RequiredArgsConstructor
@Slf4j
public class ViewController {
    private final ViewService viewService;

    @PostMapping("/ps/{documentId}")
    public ResponseEntity<?> incrementView(
            @PathVariable String documentId,
            HttpServletRequest request
    ) {
        try {
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");

            ViewDTO viewDTO = ViewDTO.builder()
                    .documentId(documentId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referer(referer)
                    .build();
            boolean increased = viewService.incrementView(viewDTO);


//            Map<String, Object> response = new HashMap<>();
//            response.put("documentId", documentId);
//            response.put("increased", increased);
//            response.put("totalViews", viewService.getTotalViews(documentId));

            IncreaseViewRequest viewRequest = new IncreaseViewRequest();
            viewRequest.setDocumentId(documentId);
            viewRequest.setIncreased(increased);
            viewRequest.setTotalViews(viewService.getTotalViews(documentId));

            return ResponseEntity.ok(viewRequest);
        } catch (Exception e) {
            log.error("조회수 증가 실패: {}", documentId, e);
            return ResponseEntity.internalServerError()
                    .body("조회수 증가에 실패했습니다.");
        }
    }

    /**
     * 특정 문서의 조회수 조회 (비인증 허용)
     */
    @GetMapping("/ps/{documentId}")
    public ResponseEntity<?> getViews(@PathVariable String documentId) {
        try {
//            Map<String, Object> response = new HashMap<>();
//            response.put("documentId", documentId);
//            response.put("totalViews", viewService.getTotalViews(documentId));
//            response.put("todayViews", viewService.getTodayViews(documentId));

            DocumentViewResponse documentViewResponse = new DocumentViewResponse();
            documentViewResponse.setDocumentId(documentId);
            documentViewResponse.setTotalViews(viewService.getTotalViews(documentId));
            documentViewResponse.setTodayViews(viewService.getTodayViews(documentId));
            return ResponseEntity.ok(documentViewResponse);
        } catch (Exception e) {
            log.error("조회수 조회 실패: {}", documentId, e);
            return ResponseEntity.internalServerError()
                    .body("조회수 조회에 실패했습니다.");
        }
    }

    /**
     * 특정 문서의 상세 조회 통계 (비인증 허용)
     */
    @GetMapping("/ps/{documentId}/stats")
    public ResponseEntity<?> getViewStats(@PathVariable String documentId) {
        try {
            ViewStatsDTO stats = viewService.getViewStats(documentId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("조회 통계 조회 실패: {}", documentId, e);
            return ResponseEntity.internalServerError()
                    .body("조회 통계 조회에 실패했습니다.");
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * 프록시/로드밸런서 환경 고려
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For는 여러 IP가 있을 수 있음 (첫 번째가 원본)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
