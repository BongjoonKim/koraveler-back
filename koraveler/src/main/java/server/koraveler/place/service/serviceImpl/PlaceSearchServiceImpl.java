package server.koraveler.place.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import server.koraveler.place.dto.PlaceDTO;
import server.koraveler.place.dto.PlaceSearchRequest;
import server.koraveler.place.dto.PlaceSearchResponse;
import server.koraveler.place.model.entities.PlaceSearchHistory;
import server.koraveler.place.repo.PlaceSearchHistoryRepo;
import server.koraveler.place.service.PlaceSearchService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSearchServiceImpl implements PlaceSearchService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlaceSearchHistoryRepo placeSearchHistoryRepo;

    @Value("${spring.kakao.api}")
    private String kakaoApiKey;

    @Value("${spring.openai.url}")
    private String openAiUrl;

    @Value("${spring.openai.key}")
    private String openAiKey;

    @Value("${spring.openai.translation.model}")
    private String aiModel;

    @Value("${spring.naver.map.client-id}")
    private String naverClientId;

    @Value("${spring.naver.map.client-secret}")
    private String naverClientSecret;

    @Override
    public PlaceSearchResponse searchPlaces(String userId, PlaceSearchRequest request) {
        log.info("Starting place search for user: {}, keyword: {}", userId, request.getKeyword());

        try {
            // 0. 다국어로된 질문을 한국어로 번역하기
            String translatedKeyword = translateKorean(request.getKeyword() != null ? request.getKeyword() : "");

            // 1. 카카오 API로 장소 검색
            List<Map<String, Object>> kakaoResults = searchKakaoPlaces(translatedKeyword);

            if (kakaoResults.isEmpty()) {
                log.info("No results found for original keyword: {}", request.getKeyword());
                log.info("No results found for translated keyword: {}", translatedKeyword);

                return PlaceSearchResponse.builder()
                        .keyword(request.getKeyword())
                        .translatedKeyword(translatedKeyword)
                        .places(new ArrayList<>())
                        .totalCount(0)
                        .build();
            }

            // 2. 검색 결과 처리 및 번역
            List<PlaceDTO> places = processSearchResults(kakaoResults, userId, request.getKeyword(), translatedKeyword);

            return PlaceSearchResponse.builder()
                    .keyword(request.getKeyword())
                    .places(places)
                    .totalCount(places.size())
                    .build();

        } catch (Exception e) {
            log.error("Error searching places: ", e);
            throw new RuntimeException("Failed to search places", e);
        }
    }

    private List<Map<String, Object>> searchKakaoPlaces(String keyword) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", keyword)
                    .queryParam("size", 15)
                    .build()
                    .encode()
                    .toUri();

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("documents")) {
                return (List<Map<String, Object>>) responseBody.get("documents");
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error calling Kakao API: ", e);
            return new ArrayList<>();
        }
    }

    private List<PlaceDTO> processSearchResults(List<Map<String, Object>> kakaoResults, String userId, String keyword, String translatedKeyword) {
        List<PlaceDTO> places = new ArrayList<>();

        for (Map<String, Object> kakaoPlace : kakaoResults) {
            try {
                // 카카오 데이터 추출
                String placeId = (String) kakaoPlace.get("id");
                String placeName = (String) kakaoPlace.get("place_name");
                String address = (String) kakaoPlace.get("address_name");
                String roadAddress = (String) kakaoPlace.get("road_address_name");
                String category = (String) kakaoPlace.get("category_group_name");
                String phone = (String) kakaoPlace.get("phone");
                double lat = Double.parseDouble((String) kakaoPlace.get("y"));
                double lng = Double.parseDouble((String) kakaoPlace.get("x"));

                // 주소를 제외한 정보 번역 (place_name, category)
                Map<String, String> translatedInfo = translatePlaceInfo(placeName, category);

                // 네이버 지오코딩으로 영문 주소 가져오기
                Map<String, Object> geoInfo = getNaverGeocode(lng, lat);

                PlaceDTO place = PlaceDTO.builder()
                        .id(placeId)
                        .name(placeName)
                        .nameEn(translatedInfo.get("name"))
                        .category(category)
                        .categoryEn(translatedInfo.get("category"))
                        .phone(phone)
                        .addressKo(address)
                        .roadAddressKo(roadAddress)
                        .addressEn((String) geoInfo.get("englishAddress"))
                        .lat(lat)
                        .lng(lng)
                        .build();

                places.add(place);

                // 검색 이력 저장 (비동기 처리 가능)
                saveSearchHistory(userId, keyword, translatedKeyword, place);

            } catch (Exception e) {
                log.warn("Failed to process place: {}", e.getMessage());
            }
        }

        return places;
    }

    private Map<String, String> translatePlaceInfo(String placeName, String category) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openAiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiModel);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are a translator. Translate the given Korean place name and category to English. " +
                            "Respond ONLY in JSON format without any markdown or code blocks: " +
                            "{\"name\": \"translated name\", \"category\": \"translated category\"}"
            );
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", String.format("Place name: %s, Category: %s",
                    placeName != null ? placeName : "",
                    category != null ? category : ""));
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_completion_tokens", 500);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    openAiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    String content = message.get("content").trim();

                    // JSON 파싱 with fallback
                    try {
                        Map<String, String> result = objectMapper.readValue(content, Map.class);
                        return result;
                    } catch (Exception e) {
                        log.warn("Failed to parse JSON response: {}", content);
                        // JSON 파싱 실패 시 fallback으로
                        Map<String, String> fallback = new HashMap<>();
                        fallback.put("name", placeName);
                        fallback.put("category", category != null ? category : "");
                        return fallback;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Translation failed, using original: {}", e.getMessage());
        }

        // 번역 실패 시 원본 반환
        Map<String, String> fallback = new HashMap<>();
        fallback.put("name", placeName);
        fallback.put("category", category != null ? category : "");
        return fallback;
    }

    private String translateKorean(String sentences) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openAiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiModel);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are a translator. Translate the given word or name or place written by some language to korean language. " +
                            "your main purpose is making easy to understand of naver map searching. " +
                            "Respond ONLY in this JSON format without any markdown or code blocks:\n" +
                            "{\"translatedKeyword\": \"translated name\"}"
            );

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", String.format("Search Keyword: %s", sentences));

            messages.add(systemMessage);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_completion_tokens", 500);

            // GPT-5 mini는 추가 파라미터 지원
            // verbosity: "low", "medium", "high" - 응답 상세도 제어
            // requestBody.put("verbosity", "low");

            // reasoning_effort: "minimal", "low", "medium", "high" - 추론 깊이 제어
            // requestBody.put("reasoning_effort", "minimal");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    openAiUrl,  // https://api.openai.com/v1/chat/completions
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    String content = message.get("content").trim();

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, String> result = mapper.readValue(content, Map.class);
                        String translated = result.get("translatedKeyword");
                        return translated != null ? translated : sentences;
                    } catch (Exception e) {
                        log.warn("Failed to parse JSON response: {}, using original", content);
                        return sentences;
                    }
                }
            }
            return sentences;
        } catch (Exception e) {
            log.error("Translation failed", e);
            return sentences;
        }
    }

    private Map<String, Object> getNaverGeocode(double lng, double lat) {
        Map<String, Object> result = new HashMap<>();
        result.put("englishAddress", ""); // 기본값

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", naverClientId);
            headers.set("X-NCP-APIGW-API-KEY", naverClientSecret);

            String coords = String.format("%f,%f", lng, lat);
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
                    .queryParam("coords", coords)
                    .queryParam("output", "json")
                    .queryParam("orders", "addr,roadaddr")
                    .build()
                    .encode()
                    .toUri();

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                if (!results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    Map<String, Object> region = (Map<String, Object>) firstResult.get("region");

                    // 영문 주소 조합
                    StringBuilder englishAddr = new StringBuilder();

                    // area1 ~ area4까지 영문명 추출
                    for (int i = 4; i >= 1; i--) {
                        Map<String, Object> area = (Map<String, Object>) region.get("area" + i);
                        if (area != null && area.containsKey("alias")) {
                            String alias = (String) area.get("alias");
                            if (alias != null && !alias.isEmpty()) {
                                if (englishAddr.length() > 0) englishAddr.append(", ");
                                englishAddr.append(alias);
                            }
                        }
                    }

                    result.put("englishAddress", englishAddr.toString());
                }
            }

        } catch (Exception e) {
            log.warn("Failed to get Naver geocode: {}", e.getMessage());
        }

        return result;
    }

    private void saveSearchHistory(String userId, String keyword, String translatedKeyword, PlaceDTO place) {
        try {
            PlaceSearchHistory history = PlaceSearchHistory.builder()
                    .userId(userId)
                    .keyword(keyword)
                    .translatedKeyword(translatedKeyword)
                    .placeId(place.getId())
                    .name(place.getName())
                    .nameEn(place.getNameEn())
                    .category(place.getCategory())
                    .categoryEn(place.getCategoryEn())
                    .phone(place.getPhone())
                    .addressKo(place.getAddressKo())
                    .roadAddressKo(place.getRoadAddressKo())
                    .addressEn(place.getAddressEn())
                    .lat(place.getLat())
                    .lng(place.getLng())
                    .searchCount(1)
                    .lastSearchedAt(LocalDateTime.now())
                    .build();

            history.setCreated(LocalDateTime.now());
            history.setUpdated(LocalDateTime.now());

            placeSearchHistoryRepo.save(history);
        } catch (Exception e) {
            log.warn("Failed to save search history: {}", e.getMessage());
        }
    }
}