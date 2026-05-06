package server.nadeliv.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OgMetadataDTO {
    private String url;
    private String title;
    private String description;
    private String image;
    private String favicon;
    private String siteName;
}
