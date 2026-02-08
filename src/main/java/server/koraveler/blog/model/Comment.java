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

@Document(collection = "comments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Comment extends CommonDTO {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String documentId;

    private String userId;
    private String content;

    private String parentId;
    private int depth; // 댓글 - 대댓글 - 대대댓글까지 가능

    private boolean isDeleted;
    private boolean isEdited;
}
