package server.koraveler.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.koraveler.blog.model.CommentLike;
import server.koraveler.blog.repo.CommentLikesRepo;
import server.koraveler.blog.repo.CommentsRepo;
import server.koraveler.blog.service.CommentLikeService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikesRepo commentLikesRepo;
    private final CommentsRepo commentsRepo;

    @Override
    @Transactional
    public boolean toggleLike(String commentId, String usersId) {
        // 댓글 존재 여부 확인
        if (!commentsRepo.existsById(commentId)) {
            throw new IllegalArgumentException("Comment not found: " + commentId);
        }

        boolean alreadyLiked = commentLikesRepo.existsByCommentIdAndUsersId(commentId, usersId);

        if (alreadyLiked) {
            removeLike(commentId, usersId);
            log.info("Like removed - commentId: {}, usersId: {}", commentId, usersId);
            return false;
        } else {
            addLike(commentId, usersId);
            log.info("Like added - commentId: {}, usersId: {}", commentId, usersId);
            return true;
        }
    }

    @Override
    @Transactional
    public CommentLike addLike(String commentId, String usersId) {
        // 이미 좋아요 했는지 확인
        if (commentLikesRepo.existsByCommentIdAndUsersId(commentId, usersId)) {
            throw new IllegalStateException("Already liked this comment");
        }

        CommentLike commentLike = CommentLike.builder()
                .commentId(commentId)
                .usersId(usersId)
                .build();

        return commentLikesRepo.save(commentLike);
    }

    @Override
    @Transactional
    public void removeLike(String commentId, String usersId) {
        commentLikesRepo.deleteByCommentIdAndUsersId(commentId, usersId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLiked(String commentId, String usersId) {
        return commentLikesRepo.existsByCommentIdAndUsersId(commentId, usersId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(String commentId) {
        return commentLikesRepo.countByCommentId(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getLikedCommentIds(List<String> commentIds, String usersId) {
        if (commentIds == null || commentIds.isEmpty() || usersId == null) {
            return Set.of();
        }

        return commentLikesRepo.findByCommentIdInAndUsersId(commentIds, usersId)
                .stream()
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void deleteAllByCommentId(String commentId) {
        commentLikesRepo.deleteByCommentId(commentId);
        log.info("All likes deleted for commentId: {}", commentId);
    }
}
