package server.nadeliv.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLikeDTO {
    private String id;
    private String documentId;
    private String usersId;
    private LocalDateTime createdAt;

    // 응답용 필드
    private boolean isLiked;
    private long likeCount;
}