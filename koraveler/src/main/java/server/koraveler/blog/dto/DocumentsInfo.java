// DocumentsInfo.java
package server.koraveler.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentsInfo {
    private long totalDocsCnt;
    private int totalPagesCnt;
    private List<DocumentsDTO> documents;  // 독립 클래스 참조
}