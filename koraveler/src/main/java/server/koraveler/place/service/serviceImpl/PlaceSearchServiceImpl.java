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
                    .queryParam("size", 10)
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

        // for문 전에 배치 준비
        List<Map<String, String>> placeInfoList = new ArrayList<>();
        for (Map<String, Object> kakaoPlace : kakaoResults) {
            Map<String, String> info = new HashMap<>();
            info.put("name", (String) kakaoPlace.get("place_name"));
            info.put("category", (String) kakaoPlace.get("category_group_name"));
            placeInfoList.add(info);
        }

        // 한 번에 번역
        List<Map<String, String>> translatedInfoList = translatePlaceInfoBatch(placeInfoList);

        for (int i = 0; i < kakaoResults.size(); i++) {
            Map<String, Object> kakaoPlace = kakaoResults.get(i);
            Map<String, String> translatedInfo = translatedInfoList.get(i);

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

                // 네이버 지오코딩으로 영문 주소 가져오기
                // 도로명 주소가 있으면 우선 사용, 없으면 지번 주소 사용
                String searchAddress = (roadAddress != null && !roadAddress.isEmpty()) ? roadAddress : address;
                Map<String, Object> geoInfo = getNaverGeocode(searchAddress, lng, lat);

                // 영문 주소 추출
                String englishAddress = (String) geoInfo.get("englishAddress");

                // 더 정확한 좌표가 있으면 업데이트
                if (geoInfo.containsKey("lat") && geoInfo.containsKey("lng")) {
                    lat = (Double) geoInfo.get("lat");
                    lng = (Double) geoInfo.get("lng");
                }

                // 영문 주소가 없으면 GPT로 번역
                if (englishAddress == null || englishAddress.isEmpty()) {
                    log.warn("No English address from Naver API for: {}, using GPT translation", searchAddress);
                    englishAddress = translateAddress(searchAddress);
                }

                PlaceDTO place = PlaceDTO.builder()
                        .id(placeId)
                        .name(placeName)
                        .nameEn(translatedInfo.get("name"))
                        .category(category)
                        .categoryEn(translatedInfo.get("category"))
                        .phone(phone)
                        .addressKo(address)
                        .roadAddressKo(roadAddress)
                        .addressEn(englishAddress)
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

    // 주소 번역 대체 메서드 (네이버 API가 실패한 경우)
    private String translateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }

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
                    "You are a translator specializing in Korean addresses. " +
                            "Translate the Korean address to English following standard romanization rules. " +
                            "Keep the structure: building number, street/dong name, gu, city. " +
                            "Respond ONLY with the translated address, no JSON or explanations.");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", address);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_completion_tokens", 200);

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
                    return message.get("content").trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to translate address: {}", e.getMessage());
        }

        return address; // 번역 실패시 원본 반환
    }

    private List<Map<String, String>> translatePlaceInfoBatch(List<Map<String, String>> placeInfoList){
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
                    "You are a translator. Translate the given list of Korean place names and categories to English. " +
                            "Respond ONLY in JSON format without any markdown or code blocks. " +
                            "Return an array with the same order: " +
                            "[{\"name\": \"translated name\", \"category\": \"translated category\"}, ...]");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            try {
                userMessage.put("content",
                        "Translate these Korean place names and categories to English:\n" +
                                objectMapper.writeValueAsString(placeInfoList));
            } catch (Exception e) {
                userMessage.put("content", placeInfoList.toString());
            }

            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_completion_tokens", 2000);

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

                    // JSON 파싱 부분을 수정:
                    try {
                        // List<Map>으로 파싱
                        List<Map<String, String>> result = objectMapper.readValue(
                                content,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                        );
                        return result;
                    } catch (Exception e) {
                        log.warn("Failed to parse JSON response: {}", content);
                        // fallback으로 원본 리스트 반환
                        List<Map<String, String>> fallbackList = new ArrayList<>();
                        for (Map<String, String> info : placeInfoList) {
                            Map<String, String> fallback = new HashMap<>();
                            fallback.put("name", info.get("name"));
                            fallback.put("category", info.get("category") != null ? info.get("category") : "");
                            fallbackList.add(fallback);
                        }
                        return fallbackList;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Translation failed, using original: {}", e.getMessage());
        }

        // 번역 실패 시 원본 리스트 반환 (메서드 끝부분)
        List<Map<String, String>> fallbackList = new ArrayList<>();
        for (Map<String, String> info : placeInfoList) {
            Map<String, String> fallback = new HashMap<>();
            fallback.put("name", info.get("name"));
            fallback.put("category", info.get("category") != null ? info.get("category") : "");
            fallbackList.add(fallback);
        }
        return fallbackList;

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
            requestBody.put("max_completion_tokens", 1500);

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

    private Map<String, Object> getNaverGeocode(String address, double lng, double lat) {
        Map<String, Object> result = new HashMap<>();
        result.put("englishAddress", ""); // 기본값

        // 주소가 없으면 빈 결과 반환
        if (address == null || address.isEmpty()) {
            return result;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            // 헤더 이름을 정확하게 대문자로 수정
            headers.set("X-NCP-APIGW-API-KEY-ID", naverClientId);
            headers.set("X-NCP-APIGW-API-KEY", naverClientSecret);

            // 정지오코딩 API 사용 (주소 → 좌표 + 상세정보)
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
                    .queryParam("query", address);

            // coordinate는 "경도,위도" 형식
            if (lng != 0 && lat != 0) {
                builder.queryParam("coordinate", String.format("%.6f,%.6f", lng, lat));
            }

            URI uri = builder.build().encode().toUri();

            log.debug("Naver Geocoding Request URL: {}", uri.toString());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            log.debug("Naver Geocoding Response: {}", responseBody);

            if (responseBody != null) {
                // 응답 상태 확인
                String status = (String) responseBody.get("status");

                if ("OK".equals(status)) {
                    List<Map<String, Object>> addresses = (List<Map<String, Object>>) responseBody.get("addresses");

                    if (addresses != null && !addresses.isEmpty()) {
                        Map<String, Object> firstAddress = addresses.get(0);

                        // 영문 주소 추출
                        String englishAddress = (String) firstAddress.get("englishAddress");

                        if (englishAddress != null && !englishAddress.isEmpty()) {
                            result.put("englishAddress", englishAddress);
                            log.info("Found English address: {}", englishAddress);
                        }

                        // 추가 정보도 저장 (필요 시)
                        result.put("jibunAddress", firstAddress.get("jibunAddress"));
                        result.put("roadAddress", firstAddress.get("roadAddress"));

                        // 좌표 정보 업데이트
                        if (firstAddress.containsKey("x") && firstAddress.containsKey("y")) {
                            try {
                                result.put("lng", Double.parseDouble((String) firstAddress.get("x")));
                                result.put("lat", Double.parseDouble((String) firstAddress.get("y")));
                            } catch (NumberFormatException e) {
                                log.warn("Failed to parse coordinates: x={}, y={}",
                                        firstAddress.get("x"), firstAddress.get("y"));
                            }
                        }
                    }
                } else {
                    log.warn("Geocoding failed with status: {} for address: {}", status, address);

                    // 상태별 처리
                    if ("INVALID_REQUEST".equals(status)) {
                        log.error("Invalid request parameters");
                    } else if ("NOT_FOUND".equals(status)) {
                        log.info("Address not found in Naver database: {}", address);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to geocode address: {}, error: {}", address, e.getMessage(), e);
        }

        return result;
    }

    // 역지오코딩 (좌표 → 주소) - 백업용
    private Map<String, Object> reverseGeocode(double lng, double lat) {
        Map<String, Object> result = new HashMap<>();
        result.put("englishAddress", ""); // 기본값

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-ncp-apigw-api-key-id", naverClientId);
            headers.set("x-ncp-apigw-api-key", naverClientSecret);
            headers.set("Accept-Language", "en"); // 영문 결과 요청

            String coords = String.format("%f,%f", lng, lat);
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc")
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

                    // land 정보가 있으면 사용
                    if (firstResult.containsKey("land")) {
                        Map<String, Object> land = (Map<String, Object>) firstResult.get("land");

                        // building name
                        if (land.containsKey("name") && !((String)land.get("name")).isEmpty()) {
                            englishAddr.append(land.get("name"));
                        }

                        // number1, number2
                        if (land.containsKey("number1")) {
                            if (englishAddr.length() > 0) englishAddr.append(", ");
                            englishAddr.append(land.get("number1"));
                            if (land.containsKey("number2") && !((String)land.get("number2")).isEmpty()) {
                                englishAddr.append("-").append(land.get("number2"));
                            }
                        }
                    }

                    // area1 ~ area4까지 영문명 추출
                    for (int i = 4; i >= 1; i--) {
                        Map<String, Object> area = (Map<String, Object>) region.get("area" + i);
                        if (area != null && area.containsKey("name")) {
                            String name = (String) area.get("name");
                            if (name != null && !name.isEmpty()) {
                                if (englishAddr.length() > 0) englishAddr.append(", ");
                                englishAddr.append(name);
                            }
                        }
                    }

                    result.put("englishAddress", englishAddr.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to reverse geocode: {}", e.getMessage());
        }

        return result;
    }

    // addressElements에서 영문 주소 조합
    private String buildEnglishAddress(Map<String, Object> addressData) {
        StringBuilder englishAddr = new StringBuilder();

        try {
            if (addressData.containsKey("addressElements")) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) addressData.get("addressElements");

                // 역순으로 조합 (상세 → 광역)
                List<String> parts = new ArrayList<>();

                for (Map<String, Object> element : elements) {
                    String longName = (String) element.get("longName");
                    String shortName = (String) element.get("shortName");
                    String code = (String) element.get("code");
                    List<String> types = (List<String>) element.get("types");

                    if (longName != null && !longName.isEmpty()) {
                        // 영문이 있는 경우 우선 사용
                        if (isEnglish(longName)) {
                            parts.add(longName);
                        } else if (shortName != null && isEnglish(shortName)) {
                            parts.add(shortName);
                        } else {
                            // 한글인 경우 그대로 사용 (나중에 번역 가능)
                            parts.add(longName);
                        }
                    }
                }

                // 역순으로 조합
                Collections.reverse(parts);
                englishAddr.append(String.join(", ", parts));
            }
        } catch (Exception e) {
            log.warn("Failed to build English address from elements: {}", e.getMessage());
        }

        return englishAddr.toString();
    }

    // 영문 여부 확인
    private boolean isEnglish(String text) {
        return text != null && text.matches("^[a-zA-Z0-9\\s\\-,.]+$");
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