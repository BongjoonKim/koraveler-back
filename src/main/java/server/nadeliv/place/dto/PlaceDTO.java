package server.nadeliv.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private String id;
    private String name;           // 한국어 이름
    private String nameEn;         // 영어 이름
    private String category;       // 한국어 카테고리
    private String categoryEn;     // 영어 카테고리
    private String phone;
    private String addressKo;      // 한국어 주소
    private String roadAddressKo;  // 한국어 도로명 주소
    private String addressEn;      // 영어 주소
    private double lat;
    private double lng;
}