package server.koraveler.blog.dto;

import server.koraveler.blog.model.Like;

public class LikeDTO {
    private String documentId;
    private String commentId;
    private Like.LikeType type;
}
