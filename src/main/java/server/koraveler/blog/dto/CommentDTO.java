package server.koraveler.blog.dto;

import lombok.Data;
import server.koraveler.blog.model.Comment;

import java.util.List;

@Data
public class CommentDTO extends Comment {
    private List<CommentDTO> replies;
    private boolean isLikedByMe;
    private boolean amIWriter;
    private long replyCount;
    private long likeCount;

}
