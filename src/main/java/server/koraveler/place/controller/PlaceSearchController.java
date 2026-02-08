package server.koraveler.place.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.koraveler.place.dto.PlaceSearchRequest;
import server.koraveler.place.dto.PlaceSearchResponse;
import server.koraveler.place.service.PlaceSearchService;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class PlaceSearchController {

    private final PlaceSearchService placeSearchService;

    /**
     * 키워드로 장소 검색
     */
    @PostMapping("/search")
    public ResponseEntity<PlaceSearchResponse> searchPlaces(
            @Valid @RequestBody PlaceSearchRequest request,
            Authentication authentication) {

        log.info("Place search request received for keyword: {}", request.getKeyword());

        try {
            String userId = authentication != null ? authentication.getName() : "anonymous";
            PlaceSearchResponse response = placeSearchService.searchPlaces(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Place search failed: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}