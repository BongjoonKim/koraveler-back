package server.koraveler.blog.model;

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
@Document(collection = "comment_likes")
@CompoundIndexes({
        @CompoundIndex(name = "comment_user_unique", def = "{'commentId': 1, 'usersId': 1}", unique = true),
        @CompoundIndex(name = "comment_id_idx", def = "{'commentId': 1}")
})
public class CommentLike {
    @Id
    private String id;

    private String commentId;

    private String usersId;

    @CreatedDate
    private LocalDateTime createdAt;
}
