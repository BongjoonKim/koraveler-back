package server.nadeliv.travel.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelMediaRequest {
    private String description;
    private Integer width;
    private Integer height;
    private Integer duration;
    private LocalDateTime takenAt;
}
