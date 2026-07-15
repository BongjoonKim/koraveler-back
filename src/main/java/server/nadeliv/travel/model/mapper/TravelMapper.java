package server.nadeliv.travel.model.mapper;

import org.springframework.stereotype.Component;
import server.nadeliv.travel.model.dto.TravelCreateRequest;
import server.nadeliv.travel.model.dto.TravelResponse;
import server.nadeliv.travel.model.entities.TravelMedia;
import server.nadeliv.travel.model.entities.TravelUsers;
import server.nadeliv.travel.model.entities.Travels;
import server.nadeliv.travel.model.enums.TravelStatus;
import server.nadeliv.travel.model.enums.TravelVisibility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TravelMapper {

    public Travels toEntity(TravelCreateRequest request, String userId) {
        Travels travel = Travels.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
                .visibility(request.getVisibility() != null ? request.getVisibility() : TravelVisibility.PRIVATE)
                .status(TravelStatus.PLANNING)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .destination(request.getDestination())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .schedules(new ArrayList<>())
                .channelIds(new ArrayList<>())
                .build();
        travel.setCreated(LocalDateTime.now());
        travel.setUpdated(LocalDateTime.now());
        travel.setCreatedUser(userId);
        travel.setUpdatedUser(userId);
        return travel;
    }

    public TravelResponse toResponse(Travels travel, List<TravelUsers> members) {
        List<TravelResponse.TravelMemberResponse> memberResponses = members != null
                ? members.stream().map(this::toMemberResponse).toList()
                : new ArrayList<>();

        return TravelResponse.builder()
                .id(travel.getId())
                .title(travel.getTitle())
                .description(travel.getDescription())
                .coverImageUrl(travel.getCoverImageUrl())
                .visibility(travel.getVisibility())
                .status(travel.getStatus())
                .startDate(travel.getStartDate())
                .endDate(travel.getEndDate())
                .destination(travel.getDestination())
                .tags(travel.getTags())
                .schedules(travel.getSchedules())
                .channelIds(travel.getChannelIds())
                .visitedRegionCodes(travel.getVisitedRegionCodes())
                .members(memberResponses)
                .memberCount(memberResponses.size())
                .createdUser(travel.getCreatedUser())
                .created(travel.getCreated())
                .updated(travel.getUpdated())
                .build();
    }

    public TravelResponse.TravelMemberResponse toMemberResponse(TravelUsers travelUser) {
        return TravelResponse.TravelMemberResponse.builder()
                .userId(travelUser.getUserId())
                .role(travelUser.getRole().name())
                .nickname(travelUser.getNickname())
                .joinedAt(travelUser.getJoinedAt())
                .build();
    }
}
