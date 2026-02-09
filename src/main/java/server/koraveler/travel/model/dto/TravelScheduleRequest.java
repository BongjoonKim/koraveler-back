package server.koraveler.travel.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.travel.model.embedded.TravelSchedule;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelScheduleRequest {

    @NotBlank(message = "일정 제목은 필수입니다")
    private String title;

    private Integer dayNumber;
    private LocalDate date;
    private String description;
    private List<TravelSchedule.SchedulePlace> places;
    private Integer sortOrder;
}
