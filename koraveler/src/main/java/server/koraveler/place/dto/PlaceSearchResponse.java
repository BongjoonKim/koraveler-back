package server.koraveler.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponse {
    private String keyword;
    private String translatedKeyword;
    private List<PlaceDTO> places;
    private int totalCount;
}