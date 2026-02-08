package server.koraveler.connections.bookmarks.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import server.koraveler.common.dto.CommonDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "con_bookmarks_users_documents")
public class Bookmark extends CommonDTO {
    @Id
    private String id;
    private String userId;
    @Field(targetType = FieldType.OBJECT_ID)  // 명시적으로 ObjectId 타입 지정
    private String documentId;
    private boolean isBookmarked;
}
