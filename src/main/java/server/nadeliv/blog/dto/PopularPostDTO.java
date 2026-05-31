package server.nadeliv.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * "Popular this month" 사이드바 위젯용 경량 DTO.
 * 본문/태그 등 무거운 필드는 제외하고 표시에 필요한 최소 필드만 노출.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PopularPostDTO {
    private int rank;
    private String id;
    private String title;
    private String thumbnailImgUrl;
    private long viewCount;
}
