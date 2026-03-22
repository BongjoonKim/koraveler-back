package server.nadeliv.place.service;

import server.nadeliv.place.dto.PlaceSearchRequest;
import server.nadeliv.place.dto.PlaceSearchResponse;

public interface PlaceSearchService {

    /**
     * 키워드로 장소 검색
     * @param userId 사용자 ID (검색 이력 저장용)
     * @param request 검색 요청 (키워드 포함)
     * @return 검색 결과 (번역된 정보 포함)
     */
    PlaceSearchResponse searchPlaces(String userId, PlaceSearchRequest request);
}