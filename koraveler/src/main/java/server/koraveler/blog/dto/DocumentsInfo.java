package server.koraveler.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import server.koraveler.blog.model.Documents;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentsInfo {
    private long totalContents;
    private int totalPages;
    private List<DocumentsDTO> documentsDTO;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocumentsDTO extends Documents {

    }


}
