package server.nadeliv.travel.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.nadeliv.travel.model.embedded.VisitedPlace;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlacesRequest {
    // 다녀온 장소 목록 (전체 교체 방식 — regions 와 동일 패턴)
    @NotNull
    private List<VisitedPlace> places;
}
