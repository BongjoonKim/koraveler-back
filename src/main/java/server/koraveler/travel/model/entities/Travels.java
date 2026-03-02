package server.koraveler.travel.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.CommonDTO;
import server.koraveler.travel.model.embedded.TravelSchedule;
import server.koraveler.travel.model.enums.TravelStatus;
import server.koraveler.travel.model.enums.TravelVisibility;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "travels")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Travels extends CommonDTO {
    @Id
    private String id;
    private String title;
    private String description;
    private String coverImageUrl;
    private TravelVisibility visibility;
    private TravelStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String destination;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<TravelSchedule> schedules = new ArrayList<>();

    @Builder.Default
    private List<String> channelIds = new ArrayList<>();
}
