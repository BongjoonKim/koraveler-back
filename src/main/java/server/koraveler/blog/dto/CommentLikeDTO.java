package server.koraveler.blog.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeDTO {
    private String id;
    private String commentId;
    private String usersId;
    private LocalDateTime createdAt;

    // 응답용 필드
    private boolean isLiked;
    private long likeCount;
}
