package server.nadeliv.blog.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.blog.model.Documents;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlogsRepo extends MongoRepository<Documents, String> {
    @Override
    long count();

    // 휴지통 자동 정리: deletedAt이 임계 시점보다 이전인 soft-deleted 문서 조회
    List<Documents> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime threshold);

    Page<Documents> findAllByDraftIsFalseOrDraftIsNullAndIsDeletedFalse(Pageable pageable);
    @Query("{ 'disclose': true, 'isDeleted': { '$ne': true }, '$or': [ " +
            "{ 'title': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'contents': { '$regex': ?1, '$options': 'i' } } " +
            "] }")
    Page<Documents> findByTitleOrContentsWithDisclose(String titleValue, String contentsValue, Pageable pageable);
    // Featured Ready인 문서들 조회
    Page<Documents> findByFeaturedReadyTrueAndDraftFalseAndIsDeletedFalse(Pageable pageable);

    // 현재 활성화된 Featured 문서 조회
    @Query("{ 'featuredSchedule.isActive': true, " +
            "'featuredSchedule.startDate': { $lte: ?0 }, " +
            "'featuredSchedule.endDate': { $gte: ?0 }, " +
            "'draft': false, " +
            "'isDeleted': { $ne: true } }")
    List<Documents> findActiveFeaturedDocuments(LocalDateTime currentDate);

    // 특정 기간의 Featured 문서 조회
    @Query("{ 'featuredSchedule.startDate': { $gte: ?0, $lte: ?1 }, " +
            "'featuredSchedule.isActive': true, " +
            "'isDeleted': { $ne: true } }")
    List<Documents> findFeaturedDocumentsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate);

    // 우선순위별 Featured 문서 조회
    @Query("{ 'featuredSchedule.isActive': true, " +
            "'featuredSchedule.startDate': { $lte: ?0 }, " +
            "'featuredSchedule.endDate': { $gte: ?0 }, " +
            "'isDeleted': { $ne: true } }")
    List<Documents> findActiveFeaturedDocumentsOrderByPriority(
            LocalDateTime currentDate,
            org.springframework.data.domain.Sort sort);

    // Featured가 아닌 일반 글 조회
    Page<Documents> findByDraftFalseAndFeaturedReadyFalseAndIsDeletedFalse(Pageable pageable);

    // Featured가 아닌 일반 글 검색
    Page<Documents> findByDraftFalseAndFeaturedReadyFalseAndIsDeletedFalseAndTitleContainingOrContentsContaining(
            String title, String contents, Pageable pageable);

    // Featured 설정된 모든 글 (히스토리용)
    Page<Documents> findByFeaturedReadyTrueAndIsDeletedFalse(Pageable pageable);
}