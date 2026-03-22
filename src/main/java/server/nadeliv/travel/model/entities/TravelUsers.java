package server.nadeliv.travel.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.nadeliv.common.dto.CommonDTO;
import server.nadeliv.travel.model.enums.TravelRole;

import java.time.LocalDateTime;

@Document(collection = "travel_users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TravelUsers extends CommonDTO {
    @Id
    private String id;

    @Indexed
    private String travelId;

    @Indexed
    private String userId;

    private TravelRole role;
    private String nickname;
    private LocalDateTime joinedAt;
}
