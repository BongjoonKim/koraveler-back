package server.nadeliv.discovery.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.nadeliv.discovery.dto.DiscoveryCollectRequest;
import server.nadeliv.discovery.dto.DiscoveryDigestResponse;
import server.nadeliv.discovery.service.DiscoveryService;
import server.nadeliv.users.dto.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
@Slf4j
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    /**
     * 목적지 다이제스트 조회 (비인증 허용 — ps 경로).
     * 수집 진행 중이면 jobStatus 로 폴링 판단.
     */
    @GetMapping("/ps/digest")
    public ResponseEntity<DiscoveryDigestResponse> getDigest(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "en") String locale) {
        try {
            return ResponseEntity.ok(discoveryService.getDigest(query, locale));
        } catch (Exception e) {
            log.error("Discovery 다이제스트 조회 실패: query={}, error={}", query, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 목적지 정보 수집 요청 (인증 필요).
     * 신선한 캐시가 있으면 즉시 반환, 없으면 작업 큐잉 후 상태 반환.
     */
    @PostMapping("/collect")
    public ResponseEntity<DiscoveryDigestResponse> collect(
            @RequestBody DiscoveryCollectRequest request,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String userId = userDetails.getUsername();
            DiscoveryDigestResponse response = discoveryService.requestCollection(
                    userId, request.getQuery(), request.getLocale(), force);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Discovery 수집 요청 실패: query={}, error={}",
                    request.getQuery(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
