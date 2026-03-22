package server.nadeliv.blog.dto;

import lombok.Data;
import server.nadeliv.blog.model.Documents;

@Data
public class DocumentViewResponse {
    private String DocumentId;
    private Long totalViews;
    private Long todayViews;
}
