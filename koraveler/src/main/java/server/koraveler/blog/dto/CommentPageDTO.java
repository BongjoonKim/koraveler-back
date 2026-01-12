package server.koraveler.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentPageDTO {
    private List<CommentDTO> comments;
    private long totalCount;
    private int totalPages;
    private boolean hasNext;
}
