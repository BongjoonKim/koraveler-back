package server.koraveler.blog.dto;

import lombok.Data;
import server.koraveler.blog.model.Documents;

@Data
public class DocumentViewResponse {
    private String DocumentId;
    private Long totalViews;
    private Long todayViews;
}
