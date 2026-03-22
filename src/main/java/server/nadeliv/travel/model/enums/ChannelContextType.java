package server.nadeliv.travel.model.enums;

public enum ChannelContextType {
    GENERAL,     // 일반 대화 (contextId = null)
    ITINERARY,   // 특정 일정 토론 (contextId = itineraryId)
    PLACE,       // 특정 장소 토론 (contextId = placeId)
    MEDIA,       // 사진/영상 공유 (contextId = null)
    INFO         // 여행 정보/기록 (contextId = null)
}
