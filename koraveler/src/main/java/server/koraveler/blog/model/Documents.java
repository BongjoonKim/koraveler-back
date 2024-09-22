package server.koraveler.blog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.CommonDTO;
import server.koraveler.common.dto.UserCommon;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "documents")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Documents extends CommonDTO {
    @Id
    private String id;
    private String title;
    private String contents;
    private String contentsType;
    private boolean disclose;
    private List<String> tags;
    private String folderId;
    private String color;
    private String thumbnailImgUrl;
    private boolean draft;
}
