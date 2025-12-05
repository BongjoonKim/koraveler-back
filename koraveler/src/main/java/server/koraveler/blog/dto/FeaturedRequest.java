// FeaturedRequest.java
package server.koraveler.blog.dto;

import lombok.Data;
import server.koraveler.blog.model.Documents;
import java.time.LocalDateTime;

@Data
public class FeaturedRequest {
    private Documents.FeaturedInfo featuredInfo;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}