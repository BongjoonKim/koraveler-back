package server.nadeliv.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.nadeliv.blog.model.DocumentLike;
import server.nadeliv.blog.repo.BlogsRepo;
import server.nadeliv.blog.repo.DocumentLikesRepo;
import server.nadeliv.blog.service.DocumentLikeService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentLikeServiceImpl implements DocumentLikeService {

    private final DocumentLikesRepo documentLikesRepo;
    private final BlogsRepo blogsRepo;

    @Override
    @Transactional
    public boolean toggleLike(String documentId, String usersId) {
        // 문서 존재 여부 확인
        if (!blogsRepo.existsById(documentId)) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        boolean alreadyLiked = documentLikesRepo.existsByDocumentIdAndUsersId(documentId, usersId);

        if (alreadyLiked) {
            removeLike(documentId, usersId);
            log.info("Like removed - documentId: {}, usersId: {}", documentId, usersId);
            return false;
        } else {
            addLike(documentId, usersId);
            log.info("Like added - documentId: {}, usersId: {}", documentId, usersId);
            return true;
        }
    }

    @Override
    @Transactional
    public DocumentLike addLike(String documentId, String usersId) {
        // 이미 좋아요 했는지 확인
        if (documentLikesRepo.existsByDocumentIdAndUsersId(documentId, usersId)) {
            throw new IllegalStateException("Already liked this document");
        }

        DocumentLike documentLike = DocumentLike.builder()
                .documentId(documentId)
                .usersId(usersId)
                .build();

        return documentLikesRepo.save(documentLike);
    }

    @Override
    @Transactional
    public void removeLike(String documentId, String usersId) {
        documentLikesRepo.deleteByDocumentIdAndUsersId(documentId, usersId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLiked(String documentId, String usersId) {
        return documentLikesRepo.existsByDocumentIdAndUsersId(documentId, usersId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(String documentId) {
        return documentLikesRepo.countByDocumentId(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getLikedDocumentIds(List<String> documentIds, String usersId) {
        if (documentIds == null || documentIds.isEmpty() || usersId == null) {
            return Set.of();
        }

        return documentLikesRepo.findByDocumentIdInAndUsersId(documentIds, usersId)
                .stream()
                .map(DocumentLike::getDocumentId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void deleteAllByDocumentId(String documentId) {
        documentLikesRepo.deleteByDocumentId(documentId);
        log.info("All likes deleted for documentId: {}", documentId);
    }
}