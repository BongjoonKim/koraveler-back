package server.koraveler.travel.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelSchedule {
    private String id;
    private Integer dayNumber;
    private LocalDate date;
    private String title;
    private String description;
    private List<SchedulePlace> places;
    private Integer sortOrder;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SchedulePlace {
        private String name;
        private String address;
        private Double lat;
        private Double lng;
        private String memo;
    }
}
