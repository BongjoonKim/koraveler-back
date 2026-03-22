// FeaturedRequest.java
package server.nadeliv.blog.dto;

import lombok.Data;
import server.nadeliv.blog.model.Documents;
import java.time.LocalDateTime;

@Data
public class FeaturedRequest {
    private Documents.FeaturedInfo featuredInfo;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}