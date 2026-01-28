package server.koraveler.blog.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.CommentDTO;
import server.koraveler.blog.dto.CommentPageDTO;
import server.koraveler.blog.model.Comment;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.blog.repo.CommentsRepo;
import server.koraveler.blog.service.CommentService;
import server.koraveler.users.repo.UsersRepo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentsRepo commentsRepo;

    @Autowired
    private BlogsRepo blogsRepo;

    @Override
    public CommentDTO createComment(CommentDTO commentDTO, String userId) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // 문서 존재 확인
        if (!blogsRepo.existsById(commentDTO.getDocumentId())) {
            throw new Exception("Cannot find Document");
        }

        Comment comment = new Comment();
        comment.setDocumentId(commentDTO.getDocumentId());
        comment.setUserId(userId);
        comment.setContent(commentDTO.getContent());
        comment.setDeleted(false);
        comment.setEdited(false);
        comment.setCreated(now);
        comment.setUpdated(now);
        comment.setCreatedUser(userId);
        comment.setUpdatedUser(userId);

        if (commentDTO.getParentId() != null) {
            Comment parentComment = commentsRepo.findById(commentDTO.getParentId())
                    .orElseThrow(() -> new Exception("Cannot find parent reply"));

            if (parentComment.getDepth() == 2) {
                Comment grandParent = commentsRepo.findById(parentComment.getParentId())
                        .orElseThrow(() -> new Exception("Cannot find parent reply"));
                comment.setParentId(grandParent.getId());
                comment.setDepth(2);
            } else if (parentComment.getDepth() == 1) {
                // depth 1에 대댓글 -> depth 2
                comment.setParentId(parentComment.getId());
                comment.setDepth(2);
            } else {
                // depth 0에 대댓글 -> depth 1
                comment.setParentId(parentComment.getId());
                comment.setDepth(parentComment.getDepth() + 1);
            }
        } else {
            // 원 댓글
            comment.setParentId(null);
            comment.setDepth(0);
        }

        Comment savedComment = commentsRepo.save(comment);
        CommentDTO resultDTO = convertCommentToDTO(savedComment, userId);
        resultDTO.setAmIWriter(true);
        resultDTO.setLikedByMe(false);
        resultDTO.setReplies(new ArrayList<>());

        log.info("created comment: commentId={}, documentId={}, userId={}",
                savedComment.getId(), savedComment.getDocumentId(), userId);

        return resultDTO;
    };

    @Override
    public CommentDTO updateComment(String commentId, CommentDTO commentDTO, String userId) throws Exception {
        Comment comment = commentsRepo.findById(commentId)
                .orElseThrow(() -> new Exception("Cannot find comment"));

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new Exception("There is no autorized to update this comment");
        }

        // 삭제된 댓글은 수정 불가
        if (comment.isDeleted()) {
            throw new Exception("Deleted comment cannot be updated");
        }

        comment.setContent(commentDTO.getContent());
        comment.setEdited(true);
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedUser(userId);

        Comment updatedComment = commentsRepo.save(comment);

        CommentDTO resultDTO = convertCommentToDTO(updatedComment, userId);
        resultDTO.setAmIWriter(true);

        log.info("댓글 수정 완료: commentId={}", commentId);

        return resultDTO;
    }

    @Override
    public void deleteComment(String commentId, String userId) throws Exception {
        Comment comment = commentsRepo.findById(commentId)
                .orElseThrow(() -> new Exception("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new Exception("댓글 삭제 권한이 없습니다.");
        }

        // soft delete
        comment.setDeleted(true);
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedUser(userId);

        commentsRepo.save(comment);

        log.info("댓글 삭제 완료: commentId={}", commentId);
    }

    @Override
    public CommentDTO hideComment(String commentId, String userId) throws Exception {
        Comment comment = commentsRepo.findById(commentId)
                .orElseThrow(() -> new Exception("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new Exception("댓글 숨김 권한이 없습니다.");
        }

        comment.setDeleted(true);
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedUser(userId);

        Comment hiddenComment = commentsRepo.save(comment);

        CommentDTO resultDTO = convertCommentToDTO(hiddenComment, userId);
        resultDTO.setAmIWriter(true);

        log.info("댓글 숨김 처리 완료: commentId={}", commentId);

        return resultDTO;
    }

    @Override
    public CommentDTO unhideComment(String commentId, String userId) throws Exception {
        Comment comment = commentsRepo.findById(commentId)
                .orElseThrow(() -> new Exception("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new Exception("댓글 숨김 해제 권한이 없습니다.");
        }

        comment.setDeleted(false);
        comment.setUpdated(LocalDateTime.now());
        comment.setUpdatedUser(userId);

        Comment unhiddenComment = commentsRepo.save(comment);

        CommentDTO resultDTO = convertCommentToDTO(unhiddenComment, userId);
        resultDTO.setAmIWriter(true);

        log.info("댓글 숨김 해제 완료: commentId={}", commentId);

        return resultDTO;
    }

    @Override
    public CommentPageDTO getRootComments(String documentId, String userId, Pageable pageable) throws Exception {
        // depth 0인 댓글만 조회
        Page<Comment> commentPage = commentsRepo.findByDocumentIdAndDepthAndDeletedFalse(
                documentId, 0, pageable);

        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(comment -> convertCommentToDTO(comment, userId))
                .collect(Collectors.toList());

        CommentPageDTO result = new CommentPageDTO();
        result.setComments(commentDTOs);
        result.setTotalCount(commentPage.getTotalElements());
        result.setTotalPages(commentPage.getTotalPages());
        result.setHasNext(commentPage.hasNext());

        return result;
    }

    @Override
    public List<CommentDTO> getReplies(String parentId, String userId) throws Exception {
        // 해당 부모의 직접 대댓글들 조회
        List<Comment> replies = commentsRepo.findByParentIdAndDeletedFalse(parentId);

        List<CommentDTO> replyDTOs = new ArrayList<>();

        for (Comment reply : replies) {
            CommentDTO replyDTO = convertCommentToDTO(reply, userId);

            // depth 1인 경우 하위 대대댓글(depth 2)도 조회
            if (reply.getDepth() == 1) {
                List<Comment> subReplies = commentsRepo.findByParentIdAndDeletedFalse(reply.getId());
                List<CommentDTO> subReplyDTOs = subReplies.stream()
                        .map(subReply -> convertCommentToDTO(subReply, userId))
                        .collect(Collectors.toList());
                replyDTO.setReplies(subReplyDTOs);
            } else {
                replyDTO.setReplies(new ArrayList<>());
            }

            replyDTOs.add(replyDTO);
        }

        return replyDTOs;
    }

    @Override
    public CommentDTO getComment(String commentId, String userId) throws Exception {
        Comment comment = commentsRepo.findById(commentId)
                .orElseThrow(() -> new Exception("댓글을 찾을 수 없습니다."));

        return convertCommentToDTO(comment, userId);
    }



    // Comment -> CommentDTO 변환 헬퍼 메서드
    private CommentDTO convertCommentToDTO(Comment comment, String userId) {
        CommentDTO dto = new CommentDTO();
        BeanUtils.copyProperties(comment, dto);

        // 작성자 여부
        dto.setAmIWriter(userId != null && userId.equals(comment.getUserId()));

        // 좋아요 여부는 LikeService에서 별도로 처리 (일단 false로 설정)
        dto.setLikedByMe(false);

        // 대댓글 목록 초기화
        dto.setReplies(new ArrayList<>());

        return dto;
    }
}
