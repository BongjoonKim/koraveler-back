package server.koraveler.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ViewDTO {
    private String documentId;
    private String ipAddress;
    private String userAgent;
    private String referer;
}