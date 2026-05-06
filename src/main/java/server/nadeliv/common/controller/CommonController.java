package server.nadeliv.common.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import server.nadeliv.common.dto.OgMetadataDTO;
import server.nadeliv.common.service.CommonService;

@RestController
@RequestMapping("ps/commons")
@Slf4j
public class CommonController {
    @Autowired
    private CommonService commonService;

    @GetMapping("/weather")
    public ResponseEntity<String> getWeather() {
        try {
            return ResponseEntity.ok(commonService.getWeatherData());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    /**
     * URL의 OpenGraph/메타데이터를 조회합니다.
     * 에디터에서 북마크 카드/인라인 멘션 링크 생성 시 사용.
     */
    @GetMapping("/og-metadata")
    public ResponseEntity<OgMetadataDTO> getOgMetadata(@RequestParam("url") String url) {
        try {
            return ResponseEntity.ok(commonService.getOgMetadata(url));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.warn("OG 메타데이터 조회 실패 url={} msg={}", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "링크 정보를 가져오지 못했습니다.");
        }
    }
}
