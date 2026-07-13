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

    // 발행 글(draft != true) 중 휴지통 제외(isDeleted != true).
    // ⚠️ 파생 쿼리 이름은 And가 Or보다 먼저 묶여 (draft=false) OR (draft=null AND isDeleted=false) 로
    // 파싱되어, 발행 글의 soft delete가 무시되는 버그가 있었음. 명시적 @Query로 isDeleted를 항상 적용.
    @Query("{ 'draft': { $ne: true }, 'isDeleted': { $ne: true } }")
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

    // sitemap.xml 용: 발행·공개 글만, 필요한 필드만 프로젝션
    @Query(value = "{ 'draft': { $ne: true }, 'isDeleted': { $ne: true }, 'disclose': { $ne: false } }",
            fields = "{ '_id': 1, 'updated': 1, 'created': 1, 'originalLocale': 1 }")
    List<Documents> findAllForSitemap();
}