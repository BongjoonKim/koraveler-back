package server.koraveler.folders.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import server.koraveler.common.dto.CommonDTO;

@Document(collection = "folders")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Folders extends CommonDTO {
    @Id
    private String id;

    private String name;

    @Indexed
    private String path;

    private String parentId;

    @Indexed
    private String userId;

    private boolean isPublic;

    private String description;;
}
