package server.koraveler.travel.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.koraveler.travel.model.enums.TravelRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelMemberRequest {

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    private TravelRole role;
    private String nickname;
}
