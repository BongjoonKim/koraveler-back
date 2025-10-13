package server.koraveler.place.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.CommonDTO;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "place_search_history")
public class PlaceSearchHistory extends CommonDTO {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String keyword;

    private String placeId;        // 카카오 place id
    private String name;
    private String nameEn;
    private String category;
    private String categoryEn;
    private String phone;
    private String addressKo;
    private String roadAddressKo;
    private String addressEn;
    private double lat;
    private double lng;

    @Builder.Default
    private int searchCount = 1;   // 검색 횟수

    private LocalDateTime lastSearchedAt;
}
