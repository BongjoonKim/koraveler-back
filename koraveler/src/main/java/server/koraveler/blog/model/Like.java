package server.koraveler.blog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import server.koraveler.common.dto.CommonDTO;

@Document(collection = "likes")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Like extends CommonDTO {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String documentId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String commentId;

    private String userId;

    public enum LikeType {
        DOCUMENT,
        COMMENT
    }
}
