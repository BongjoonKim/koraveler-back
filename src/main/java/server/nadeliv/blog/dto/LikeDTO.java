package server.nadeliv.blog.dto;

import server.nadeliv.blog.model.Like;

public class LikeDTO {
    private String documentId;
    private String commentId;
    private Like.LikeType type;
}
