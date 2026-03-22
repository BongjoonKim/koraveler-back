package server.nadeliv.blog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_likes")
@CompoundIndexes({
        @CompoundIndex(name = "document_user_unique", def = "{'documentId': 1, 'usersId': 1}", unique = true),
        @CompoundIndex(name = "document_id_idx", def = "{'documentId': 1}")
})
public class DocumentLike {
    @Id
    private String id;

    private String documentId;

    private String usersId;

    @CreatedDate
    private LocalDateTime createdAt;
}