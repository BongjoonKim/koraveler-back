package server.nadeliv.travel.model.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.nadeliv.travel.model.embedded.DashboardItem;
import server.nadeliv.travel.model.enums.TravelStatus;
import server.nadeliv.travel.model.enums.TravelVisibility;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelUpdateRequest {

    @Size(max = 100, message = "여행 제목은 100자를 초과할 수 없습니다")
    private String title;

    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다")
    private String description;

    private String coverImageUrl;
    private TravelVisibility visibility;
    private TravelStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String destination;
    private List<String> tags;
    private List<DashboardItem> dashboardItems;
}
