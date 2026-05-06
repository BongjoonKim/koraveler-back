package server.nadeliv.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.nadeliv.blog.model.Comment;
import server.nadeliv.blog.model.Documents;
import server.nadeliv.blog.repo.BlogsRepo;
import server.nadeliv.blog.repo.CommentLikesRepo;
import server.nadeliv.blog.repo.CommentsRepo;
import server.nadeliv.blog.repo.DocumentLikesRepo;
import server.nadeliv.connections.bookmarks.model.Bookmark;
import server.nadeliv.blog.model.DocumentView;
import server.nadeliv.i18n.repo.PostTranslationRepo;
import server.nadeliv.travel.service.S3Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 휴지통(soft delete)에 들어간 블로그 글을 일정 기간 후 영구 삭제하는 스케줄러.
 *
 * 동작:
 *  1. 매일 새벽 4시(서버 로컬 타임존)에 실행
 *  2. deletedAt이 RETENTION_DAYS 이전인 Documents 조회
 *  3. 본문 HTML과 thumbnailImgUrl에서 S3 객체 URL 추출 → S3에서 삭제
 *  4. 댓글/댓글 좋아요/문서 좋아요/조회기록/북마크/번역 cascade 삭제
 *  5. Documents 본체 영구 삭제
 *
 * 휴지통 보관 기간을 바꾸려면 RETENTION_DAYS 값만 조정.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BlogCleanupScheduler {

    // 휴지통 보관 기간 (일). 90일 = 약 3개월
    public static final int RETENTION_DAYS = 90;

    // 블로그 이미지/영상이 저장되는 S3 버킷 호스트 식별자
    private static final Set<String> BLOG_S3_BUCKET_HOSTS = Set.of(
            "haries-img",
            "haries-thumbnail"
    );

    private final BlogsRepo blogsRepo;
    private final CommentsRepo commentsRepo;
    private final CommentLikesRepo commentLikesRepo;
    private final DocumentLikesRepo documentLikesRepo;
    private final PostTranslationRepo postTranslationRepo;
    private final MongoTemplate mongoTemplate;
    private final S3Service s3Service;

    /**
     * 매일 새벽 4:00 실행. cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeExpiredTrash() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<Documents> targets = blogsRepo.findByIsDeletedTrueAndDeletedAtBefore(threshold);

        if (targets.isEmpty()) {
            log.info("Blog trash purge: no expired documents (threshold={})", threshold);
            return;
        }

        log.info("Blog trash purge: {} expired documents (threshold={})", targets.size(), threshold);
        int success = 0;
        int failed = 0;
        for (Documents doc : targets) {
            try {
                purgeDocument(doc);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to purge document id={}: {}", doc.getId(), e.getMessage(), e);
            }
        }
        log.info("Blog trash purge done: success={}, failed={}", success, failed);
    }

    private void purgeDocument(Documents doc) {
        String docId = doc.getId();

        // 1. S3 객체 삭제 (썸네일 + 본문 내 이미지/영상)
        Set<String> mediaUrls = collectS3MediaUrls(doc);
        for (String url : mediaUrls) {
            s3Service.deleteFileByUrl(url);
        }

        // 2. 댓글 좋아요 cascade — 각 댓글의 좋아요부터 정리
        List<Comment> comments = commentsRepo.findByDocumentIdAndDeletedFalse(docId);
        for (Comment c : comments) {
            commentLikesRepo.deleteByCommentId(c.getId());
        }
        // soft-deleted 포함 모든 댓글 정리
        mongoTemplate.remove(new Query(Criteria.where("documentId").is(docId)), Comment.class);

        // 3. 문서 좋아요
        documentLikesRepo.deleteByDocumentId(docId);

        // 4. 조회 기록
        mongoTemplate.remove(new Query(Criteria.where("documentId").is(docId)), DocumentView.class);

        // 5. 북마크
        mongoTemplate.remove(new Query(Criteria.where("documentId").is(docId)), Bookmark.class);

        // 6. 번역
        postTranslationRepo.deleteByPostId(docId);

        // 7. Documents 본체
        blogsRepo.deleteById(docId);

        log.info("Permanently purged document id={}, title='{}', mediaCount={}",
                docId, doc.getTitle(), mediaUrls.size());
    }

    /**
     * Document에 연결된 S3 객체 URL 모두 수집.
     * - thumbnailImgUrl
     * - contents HTML 안의 <img src>, <video src>, <source src>
     * - featuredInfo.featuredImageUrl
     * 블로그 전용 버킷(haries-img, haries-thumbnail)에 속한 URL만 반환.
     */
    private Set<String> collectS3MediaUrls(Documents doc) {
        Set<String> urls = new HashSet<>();

        addIfBlogBucket(urls, doc.getThumbnailImgUrl());
        if (doc.getFeaturedInfo() != null) {
            addIfBlogBucket(urls, doc.getFeaturedInfo().getFeaturedImageUrl());
        }

        String html = doc.getContents();
        if (html != null && !html.isBlank()) {
            try {
                org.jsoup.nodes.Document parsed = Jsoup.parse(html);
                Elements media = parsed.select("img[src], video[src], source[src]");
                for (Element el : media) {
                    addIfBlogBucket(urls, el.attr("src"));
                }
            } catch (Exception e) {
                log.warn("HTML parse failed for document id={}: {}", doc.getId(), e.getMessage());
            }
        }
        return urls;
    }

    private void addIfBlogBucket(Set<String> sink, String url) {
        if (url == null || url.isBlank()) return;
        for (String host : BLOG_S3_BUCKET_HOSTS) {
            if (url.contains(host)) {
                sink.add(url);
                return;
            }
        }
    }
}
