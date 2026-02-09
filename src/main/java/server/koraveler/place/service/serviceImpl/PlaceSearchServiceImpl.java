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
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.core.SdkBytes;


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
    private final TranslateClient translateClient;
    private final BedrockRuntimeClient bedrockRuntimeClient;


    @Value("${spring.kakao.api}")
    private String kakaoApiKey;

    @Value("${spring.naver.map.client-id}")
    private String naverClientId;

    @Value("${spring.naver.map.client-secret}")
    private String naverClientSecret;

    @Value("${cloud.aws.bedrock.model-id}")
    private String bedrockModelId;

    // ========================================================================
    // 검색 메인 로직
    // ========================================================================

    @Override
    public PlaceSearchResponse searchPlaces(String userId, PlaceSearchRequest request) {
        log.info("Starting place search for user: {}, keyword: {}", userId, request.getKeyword());

        try {
            String keyword = request.getKeyword() != null ? request.getKeyword() : "";
            String finalTranslatedKeyword;
            List<Map<String, Object>> kakaoResults;

            if (!isKorean(keyword) && !keyword.isBlank()) {
                // 영어 입력: Bedrock 추론 번역 우선 (문맥 기반으로 정확한 한국어 장소명 추론)
                // 예: "inducwon station" → Bedrock: "인덕원역" (AWS Translate는 "인덕원 스테이션"으로 오역)
                String bedrockTranslated = translateWithBedrock(keyword);
                finalTranslatedKeyword = bedrockTranslated;
                kakaoResults = searchKakaoPlaces(bedrockTranslated);

                // Bedrock 결과가 없으면 AWS Translate로 fallback
                if (kakaoResults.isEmpty()) {
                    log.info("Bedrock result ('{}' → '{}') returned no kakao results. Falling back to AWS Translate...",
                            keyword, bedrockTranslated);
                    String awsTranslated = translateToKorean(keyword);

                    if (!awsTranslated.equals(bedrockTranslated)) {
                        finalTranslatedKeyword = awsTranslated;
                        kakaoResults = searchKakaoPlaces(awsTranslated);
                    }
                }
            } else {
                // 한국어 입력: AWS Translate (자동 감지) 후 바로 검색
                finalTranslatedKeyword = translateToKorean(keyword);
                kakaoResults = searchKakaoPlaces(finalTranslatedKeyword);
            }

            if (kakaoResults.isEmpty()) {
                log.info("No results found for original keyword: {}", request.getKeyword());
                log.info("No results found for translated keyword: {}", finalTranslatedKeyword);

                return PlaceSearchResponse.builder()
                        .keyword(request.getKeyword())
                        .translatedKeyword(finalTranslatedKeyword)
                        .places(new ArrayList<>())
                        .totalCount(0)
                        .build();
            }

            // 3. 검색 결과 처리 및 번역
            List<PlaceDTO> places = processSearchResults(kakaoResults, userId, request.getKeyword(), finalTranslatedKeyword);

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

    // ========================================================================
    // Amazon Translate 번역 메서드
    // ========================================================================

    /**
     * Amazon Translate로 텍스트 번역
     * @param text 원본 텍스트
     * @param sourceLanguageCode 소스 언어 ("auto"로 자동 감지)
     * @param targetLanguageCode 타겟 언어 ("ko", "en" 등)
     * @return 번역된 텍스트
     */
    private String translate(String text, String sourceLanguageCode, String targetLanguageCode) {
        if (text == null || text.isBlank()) {
            return "";
        }

        try {
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .text(text)
                    .sourceLanguageCode(sourceLanguageCode)
                    .targetLanguageCode(targetLanguageCode)
                    .build();

            TranslateTextResponse response = translateClient.translateText(request);
            String translated = response.translatedText();
            log.debug("Translated [{}] '{}' → [{}] '{}'", sourceLanguageCode, text, targetLanguageCode, translated);
            return translated;

        } catch (Exception e) {
            log.warn("Amazon Translate failed for '{}': {}", text, e.getMessage());
            return text; // 실패 시 원본 반환
        }
    }

    /**
     * 검색어를 한국어로 번역 (자동 언어 감지)
     * 기존: GPT에게 JSON 형식으로 번역 요청 → 파싱
     * 변경: Amazon Translate 단일 호출 (100-200ms)
     */
    private String translateToKorean(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }

        // 이미 한국어면 그대로 반환
        if (isKorean(keyword)) {
            return keyword;
        }

        return translate(keyword, "auto", "ko");
    }

    /**
     * AWS Bedrock Claude Haiku 3.5를 사용한 추론 번역 (Fallback용)
     * 영어 발음을 한국어 장소명으로 추론하여 번역한다.
     * 예: "docklipmoon" → "독립문", "gyeongbokgoong" → "경복궁"
     *
     * AWS Translate가 단순 음역만 수행하여 검색 결과가 없을 때 1회 fallback으로 사용.
     */
    private String translateWithBedrock(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }

        try {
            String prompt = String.format(
                    "You are a Korean place name translator. The user typed a Korean place name using English letters (romanization). " +
                    "Your job is to figure out the correct Korean place name.\n\n" +
                    "Rules:\n" +
                    "- The input is an English romanization of a Korean place name.\n" +
                    "- Think about what Korean place the user is trying to say.\n" +
                    "- Consider common Korean landmarks, stations, neighborhoods, restaurants, and tourist spots.\n" +
                    "- Return ONLY the Korean place name. No explanation, no quotes, no extra text.\n\n" +
                    "Examples:\n" +
                    "- docklipmoon → 독립문\n" +
                    "- gyeongbokgoong → 경복궁\n" +
                    "- namsan tawa → 남산타워\n" +
                    "- hongdae → 홍대\n" +
                    "- itaewon → 이태원\n" +
                    "- bukchon hanok → 북촌한옥마을\n" +
                    "- myeongdong → 명동\n\n" +
                    "Input: %s\nKorean place name:",
                    keyword
            );

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "anthropic_version", "bedrock-2023-05-31",
                    "max_tokens", 100,
                    "temperature", 0.0,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            ));

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(bedrockModelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) responseMap.get("content");

            if (contentList != null && !contentList.isEmpty()) {
                String result = ((String) contentList.get(0).get("text")).trim();
                log.info("Bedrock fallback translation: '{}' → '{}'", keyword, result);
                return result;
            }

            return keyword;

        } catch (Exception e) {
            log.warn("Bedrock fallback translation failed for '{}': {}", keyword, e.getMessage());
            return keyword;
        }
    }

    /**
     * 장소명 + 카테고리를 한 번에 영어로 번역 (배치)
     * 기존: GPT에게 JSON 배열로 번역 요청 → JSON 파싱 (불안정)
     * 변경: 구분자 방식으로 Amazon Translate 1회 호출
     *
     * Amazon Translate는 최대 10,000 바이트까지 한 번에 처리 가능.
     * 장소 10개 × (이름 + 카테고리) 정도는 충분히 1회 호출로 처리됨.
     */
    private List<Map<String, String>> translatePlaceInfoBatch(List<Map<String, String>> placeInfoList) {
        if (placeInfoList == null || placeInfoList.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 1. 모든 텍스트를 구분자로 연결: "이름1|||카테고리1|||이름2|||카테고리2..."
            List<String> allTexts = new ArrayList<>();
            for (Map<String, String> info : placeInfoList) {
                allTexts.add(info.getOrDefault("name", ""));
                allTexts.add(info.getOrDefault("category", ""));
            }

            String combined = String.join(" ||| ", allTexts);

            // 2. Amazon Translate 1회 호출
            String translated = translate(combined, "ko", "en");

            // 3. 구분자로 분리하여 다시 매핑
            // Amazon Translate가 ||| 를 || 로 변환하거나 공백을 넣을 수 있으므로
            // 파이프 2개 이상 연속(공백 포함)을 구분자로 인식
            String[] translatedParts = translated.split("\\s*(?:\\|\\s*){2,}\\s*");
            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 0; i < placeInfoList.size(); i++) {
                Map<String, String> translatedInfo = new HashMap<>();
                int nameIdx = i * 2;
                int categoryIdx = i * 2 + 1;

                String translatedName = nameIdx < translatedParts.length
                        ? translatedParts[nameIdx].trim().replaceAll("\\|", "").trim()
                        : placeInfoList.get(i).get("name");
                String translatedCategory = categoryIdx < translatedParts.length
                        ? translatedParts[categoryIdx].trim().replaceAll("\\|", "").trim()
                        : placeInfoList.get(i).getOrDefault("category", "");

                // 번역 결과에 여전히 한국어가 포함되어 있으면 개별 번역 시도
                if (isKorean(translatedName)) {
                    translatedName = translate(placeInfoList.get(i).get("name"), "ko", "en");
                }
                if (isKorean(translatedCategory)) {
                    translatedCategory = translate(placeInfoList.get(i).getOrDefault("category", ""), "ko", "en");
                }

                translatedInfo.put("name", translatedName);
                translatedInfo.put("category", translatedCategory);

                result.add(translatedInfo);
            }

            return result;

        } catch (Exception e) {
            log.warn("Batch translation failed, returning originals: {}", e.getMessage());

            // 실패 시 원본 반환
            List<Map<String, String>> fallbackList = new ArrayList<>();
            for (Map<String, String> info : placeInfoList) {
                Map<String, String> fallback = new HashMap<>();
                fallback.put("name", info.get("name"));
                fallback.put("category", info.getOrDefault("category", ""));
                fallbackList.add(fallback);
            }
            return fallbackList;
        }
    }

    /**
     * 한국어 주소를 영어로 번역 (네이버 API 실패 시 fallback)
     * 기존: GPT에게 주소 번역 프롬프트 → 응답 파싱
     * 변경: Amazon Translate 단일 호출
     */
    private String translateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }

        return translate(address, "ko", "en");
    }

    // ========================================================================
    // 유틸리티 메서드
    // ========================================================================

    /**
     * 텍스트에 한국어가 포함되어 있는지 확인
     */
    private boolean isKorean(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO ||
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }

    /**
     * 영문 여부 확인
     */
    private boolean isEnglish(String text) {
        return text != null && text.matches("^[a-zA-Z0-9\\s\\-,.]+$");
    }

    // ========================================================================
    // 카카오 장소 검색
    // ========================================================================

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

    // ========================================================================
    // 검색 결과 처리
    // ========================================================================

    private List<PlaceDTO> processSearchResults(List<Map<String, Object>> kakaoResults, String userId, String keyword, String translatedKeyword) {
        List<PlaceDTO> places = new ArrayList<>();

        // 배치 번역 준비
        List<Map<String, String>> placeInfoList = new ArrayList<>();
        for (Map<String, Object> kakaoPlace : kakaoResults) {
            Map<String, String> info = new HashMap<>();
            info.put("name", (String) kakaoPlace.get("place_name"));
            info.put("category", (String) kakaoPlace.get("category_group_name"));
            placeInfoList.add(info);
        }

        // 장소명 + 카테고리 한 번에 번역 (Amazon Translate 1회 호출)
        List<Map<String, String>> translatedInfoList = translatePlaceInfoBatch(placeInfoList);

        for (int i = 0; i < kakaoResults.size(); i++) {
            Map<String, Object> kakaoPlace = kakaoResults.get(i);
            Map<String, String> translatedInfo = translatedInfoList.get(i);

            try {
                String placeId = (String) kakaoPlace.get("id");
                String placeName = (String) kakaoPlace.get("place_name");
                String address = (String) kakaoPlace.get("address_name");
                String roadAddress = (String) kakaoPlace.get("road_address_name");
                String category = (String) kakaoPlace.get("category_group_name");
                String phone = (String) kakaoPlace.get("phone");
                double lat = Double.parseDouble((String) kakaoPlace.get("y"));
                double lng = Double.parseDouble((String) kakaoPlace.get("x"));

                // 네이버 지오코딩으로 영문 주소 가져오기
                String searchAddress = (roadAddress != null && !roadAddress.isEmpty()) ? roadAddress : address;
                Map<String, Object> geoInfo = getNaverGeocode(searchAddress, lng, lat);

                String englishAddress = (String) geoInfo.get("englishAddress");

                if (geoInfo.containsKey("lat") && geoInfo.containsKey("lng")) {
                    lat = (Double) geoInfo.get("lat");
                    lng = (Double) geoInfo.get("lng");
                }

                // 영문 주소가 없으면 Amazon Translate로 번역
                if (englishAddress == null || englishAddress.isEmpty()) {
                    log.warn("No English address from Naver API for: {}, using Amazon Translate", searchAddress);
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

                saveSearchHistory(userId, keyword, translatedKeyword, place);

            } catch (Exception e) {
                log.warn("Failed to process place: {}", e.getMessage());
            }
        }

        return places;
    }

    // ========================================================================
    // 네이버 지오코딩
    // ========================================================================

    private Map<String, Object> getNaverGeocode(String address, double lng, double lat) {
        Map<String, Object> result = new HashMap<>();
        result.put("englishAddress", "");

        if (address == null || address.isEmpty()) {
            return result;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", naverClientId);
            headers.set("X-NCP-APIGW-API-KEY", naverClientSecret);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
                    .queryParam("query", address);

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
                String status = (String) responseBody.get("status");

                if ("OK".equals(status)) {
                    List<Map<String, Object>> addresses = (List<Map<String, Object>>) responseBody.get("addresses");

                    if (addresses != null && !addresses.isEmpty()) {
                        Map<String, Object> firstAddress = addresses.get(0);
                        String englishAddress = (String) firstAddress.get("englishAddress");

                        if (englishAddress != null && !englishAddress.isEmpty()) {
                            result.put("englishAddress", englishAddress);
                            log.info("Found English address: {}", englishAddress);
                        }

                        result.put("jibunAddress", firstAddress.get("jibunAddress"));
                        result.put("roadAddress", firstAddress.get("roadAddress"));

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
                }
            }

        } catch (Exception e) {
            log.error("Failed to geocode address: {}, error: {}", address, e.getMessage(), e);
        }

        return result;
    }

    // ========================================================================
    // 검색 이력 저장
    // ========================================================================

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