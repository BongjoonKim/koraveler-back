package server.nadeliv.travel.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelRegionsRequest {
    // 방문한 시/군 행정코드 목록 (전체 교체 방식)
    @NotNull
    private List<String> regionCodes;
}
