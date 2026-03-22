package server.nadeliv.blog.dto;

import lombok.Data;

@Data
public class IncreaseViewRequest {
    private String documentId;
    private Boolean increased;
    private Long totalViews;
}
