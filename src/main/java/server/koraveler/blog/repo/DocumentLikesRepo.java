package server.koraveler.blog.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.blog.model.DocumentLike;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentLikesRepo extends MongoRepository<DocumentLike, String> {

    // 특정 문서에 특정 사용자가 좋아요 했는지 확인
    Optional<DocumentLike> findByDocumentIdAndUsersId(String documentId, String usersId);

    // 특정 문서에 특정 사용자가 좋아요 했는지 여부
    boolean existsByDocumentIdAndUsersId(String documentId, String usersId);

    // 특정 문서의 좋아요 삭제 (좋아요 취소)
    void deleteByDocumentIdAndUsersId(String documentId, String usersId);

    // 특정 문서의 좋아요 수 조회
    long countByDocumentId(String documentId);

    // 특정 문서의 모든 좋아요 조회 (좋아요한 사람 목록)
    List<DocumentLike> findByDocumentId(String documentId);

    // 특정 사용자가 좋아요한 모든 문서 좋아요 조회
    List<DocumentLike> findByUsersId(String usersId);

    // 여러 문서에 대해 특정 사용자가 좋아요 했는지 확인 (문서 목록 조회 시 사용)
    List<DocumentLike> findByDocumentIdInAndUsersId(List<String> documentIds, String usersId);

    // 문서 삭제 시 해당 문서의 모든 좋아요 삭제
    void deleteByDocumentId(String documentId);
}