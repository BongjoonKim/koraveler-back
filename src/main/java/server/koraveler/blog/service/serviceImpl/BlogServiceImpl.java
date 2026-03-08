package server.koraveler.blog.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import server.koraveler.blog.constants.BlogConstants;
import server.koraveler.blog.dto.DocumentsDTO;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.model.Documents;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.blog.service.BlogService;
import server.koraveler.connections.bookmarks.repo.BookmarksRepo;
import server.koraveler.i18n.service.I18nTranslationService;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;
import org.bson.Document;  // 이 import 추가 필요!


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlogServiceImpl implements BlogService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BlogsRepo blogsRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private BookmarksRepo bookmarksRepo;

    @Autowired
    private I18nTranslationService i18nTranslationService;

    private void testAggregationSteps(
            AggregationOperation matchDraft,
            AggregationOperation lookupBookmarks,
            AggregationOperation matchUserBookmarks,
            String userId) {

        // Step 1: matchDraft 테스트
        Aggregation test1 = Aggregation.newAggregation(matchDraft);
        List<Documents> step1Results = mongoTemplate.aggregate(
                test1, "documents", Documents.class
        ).getMappedResults();
        System.out.println("Step 1 - After matchDraft: " + step1Results.size() + " documents");

        // Step 2: lookup 테스트 - org.bson.Document 사용!
        Aggregation test2 = Aggregation.newAggregation(matchDraft, lookupBookmarks);
        List<Document> step2Results = mongoTemplate.aggregate(
                test2, "documents", Document.class  // org.bson.Document 사용
        ).getMappedResults();

        System.out.println("Step 2 - After lookup: " + step2Results.size() + " documents");

        // bookmarks 배열 확인
        for (Document doc : step2Results) {  // Document는 org.bson.Document
            List<Document> bookmarks = (List<Document>) doc.get("bookmarks");
            if (bookmarks != null && !bookmarks.isEmpty()) {
                System.out.println("  문서 ID: " + doc.get("_id"));
                System.out.println("  bookmarks 개수: " + bookmarks.size());

                // 각 bookmark 내용 출력
                for (Document bookmark : bookmarks) {
                    System.out.println("    - userId: " + bookmark.get("userId") +
                            ", isBookmarked: " + bookmark.get("isBookmarked") +
                            ", documentId: " + bookmark.get("documentId"));
                }
            }
        }

        // 현재 사용자의 북마크가 있는지 확인
        long userBookmarkCount = step2Results.stream()
                .filter(doc -> {
                    List<Document> bookmarks = (List<Document>) doc.get("bookmarks");
                    if (bookmarks == null) return false;
                    return bookmarks.stream().anyMatch(b ->
                            userId.equals(b.get("userId")) &&
                                    Boolean.TRUE.equals(b.get("isBookmarked"))
                    );
                })
                .count();

        System.out.println("Step 2.5 - 현재 사용자(" + userId + ")의 북마크가 있는 문서 수: " + userBookmarkCount);

        // Step 3: matchUserBookmarks 테스트
        Aggregation test3 = Aggregation.newAggregation(
                matchDraft,
                lookupBookmarks,
                matchUserBookmarks
        );
        List<Documents> step3Results = mongoTemplate.aggregate(
                test3, "documents", Documents.class  // 여기는 Documents 클래스 사용 가능
        ).getMappedResults();

        System.out.println("Step 3 - After user filter: " + step3Results.size() + " documents");

        if (step3Results.isEmpty() && userBookmarkCount > 0) {
            System.out.println("⚠️ 경고: lookup 후에는 사용자 북마크가 있었지만, elemMatch 필터 후 결과가 없습니다!");
            System.out.println("=== 대체 방법 테스트 ===");

            // 대체 방법 1: unwind 사용
            testUnwindMethod(matchDraft, lookupBookmarks, userId);
        }
    }

    // 대체 방법 테스트 메서드도 추가
    private void testUnwindMethod(
            AggregationOperation matchDraft,
            AggregationOperation lookupBookmarks,
            String userId) {

        System.out.println("=== Unwind 방법 테스트 ===");

        AggregationOperation unwind = Aggregation.unwind("bookmarks", false);
        AggregationOperation matchDirect = Aggregation.match(
                Criteria.where("bookmarks.userId").is(userId)
                        .and("bookmarks.isBookmarked").is(true)
        );

        Aggregation unwindAgg = Aggregation.newAggregation(
                matchDraft,
                lookupBookmarks,
                unwind,
                matchDirect
        );

        List<Documents> unwindResults = mongoTemplate.aggregate(
                unwindAgg, "documents", Documents.class
        ).getMappedResults();

        System.out.println("Unwind 방법 결과: " + unwindResults.size() + " documents");

        if (!unwindResults.isEmpty()) {
            System.out.println("✅ Unwind 방법이 작동합니다! elemMatch 대신 이 방법을 사용하세요.");
        }
    }

    private void testAggregationStepsWithAddFields(
            AggregationOperation matchDraft,
            AggregationOperation addFields,
            AggregationOperation lookupBookmarks,
            AggregationOperation matchUserBookmarks,
            String userId) {

        // Step 1: matchDraft 테스트
        Aggregation test1 = Aggregation.newAggregation(matchDraft);
        List<Documents> step1Results = mongoTemplate.aggregate(
                test1, "documents", Documents.class
        ).getMappedResults();
        System.out.println("Step 1 - After matchDraft: " + step1Results.size() + " documents");

        // Step 2: addFields 테스트
        Aggregation test2 = Aggregation.newAggregation(matchDraft, addFields, lookupBookmarks);
        List<Document> step2Results = mongoTemplate.aggregate(
                test2, "documents", Document.class
        ).getMappedResults();

        System.out.println("Step 2 - After addFields and lookup: " + step2Results.size() + " documents");

        // bookmarks 배열 확인
        for (Document doc : step2Results) {
            List<Document> bookmarks = (List<Document>) doc.get("bookmarks");
            if (bookmarks != null && !bookmarks.isEmpty()) {
                System.out.println("  문서 ID: " + doc.get("_id"));
                System.out.println("  문서 _idStr: " + doc.get("_idStr"));  // 변환된 string ID 확인
                System.out.println("  bookmarks 개수: " + bookmarks.size());

                for (Document bookmark : bookmarks) {
                    System.out.println("    - userId: " + bookmark.get("userId") +
                            ", isBookmarked: " + bookmark.get("isBookmarked") +
                            ", documentId: " + bookmark.get("documentId"));
                }
            }
        }

        // Step 3: 최종 필터링
        Aggregation test3 = Aggregation.newAggregation(
                matchDraft,
                addFields,
                lookupBookmarks,
                matchUserBookmarks
        );
        List<Documents> step3Results = mongoTemplate.aggregate(
                test3, "documents", Documents.class
        ).getMappedResults();

        System.out.println("Step 3 - After user filter: " + step3Results.size() + " documents");
    }

    @Override
    public DocumentsDTO createDocument(DocumentsDTO documentsDTO) {

        Documents documents = new Documents();
        BeanUtils.copyProperties(documentsDTO, documents);
        LocalDateTime now = LocalDateTime.now();

        documents.setCreated(now);
        documents.setUpdated(now);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            documents.setCreatedUser(username);
            documents.setUpdatedUser(username);
            if (documents.getFolderId() == null) {
                Users users = usersRepo.findByUserId(username);
                documents.setFolderId(users.getId());
            }
            Documents afterDocument = blogsRepo.save(documents);

            DocumentsDTO newDocDTO = new DocumentsDTO();
            BeanUtils.copyProperties(afterDocument, newDocDTO);

            return newDocDTO;
        }
        return null;
    }

    @Override
    public DocumentsDTO saveDocument(DocumentsDTO documentsDTO) {
        try {
            Documents documents = new Documents();
            BeanUtils.copyProperties(documentsDTO, documents);
            LocalDateTime now = LocalDateTime.now();

            documents.setUpdated(now);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                System.out.println("userDetails = " + userDetails);
                String username = userDetails.getUsername();
                documents.setUpdatedUser(username);
                if (documents.getFolderId() == null) {
                    Users users = usersRepo.findByUserId(username);
                    documents.setFolderId(users.getId());
                }
                Documents afterDocument = blogsRepo.save(documents);

                // i18n: 발행된 글(draft가 아닌)이면 자동 번역 큐잉
                if (!afterDocument.isDraft()) {
                    try {
                        i18nTranslationService.queueTranslations(afterDocument);
                    } catch (Exception e) {
                        log.warn("번역 큐잉 실패 (글 저장은 성공): {}", e.getMessage());
                    }
                }

                DocumentsDTO newDocDTO = new DocumentsDTO();
                BeanUtils.copyProperties(afterDocument, newDocDTO);

                return newDocDTO;
            }
            return null;
        } catch (Exception e) {
            throw e;
        }
    }


    @Override
    public DocumentsInfo getDocuments(PaginationDTO pageDTO) throws Exception {
        try {
            Page<Documents> documents = null;
            Sort.Order updatedSort = "ASC".equals(pageDTO.getDateSort()) ? Sort.Order.asc("updated") : Sort.Order.desc("updated");
            Sort sort = Sort.by(updatedSort);

            Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);

            if ("all".equals(pageDTO.getFolderId()) || pageDTO.getPage() == -1) {
                documents = blogsRepo.findAllByDraftIsFalseOrDraftIsNull(pageable);
            } else {
                if (ObjectUtils.isEmpty(pageDTO.getPageType())) {
                    documents = blogsRepo.findAllByDraftIsFalseOrDraftIsNull(pageable);
                } else {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.getPrincipal() != null) {
                        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                        String username = userDetails.getUsername();
                        Users users = usersRepo.findByUserId(username);
                        if (!ObjectUtils.isEmpty(users)) {
                            if (BlogConstants.BlogPageType.MY_BLOG.getValue().equals(pageDTO.getPageType())) {
                                documents = this.findByCreatedUserOrUpdatedUserAndDraft(users.getUserId(), users.getUserId(), pageable, false);
                            } else if (BlogConstants.BlogPageType.BOOKMARK.getValue().equals(pageDTO.getPageType())) {
                                // 디버깅 모드
                                boolean debugMode = false; // 배포 시 false로 변경

                                if (debugMode) {
                                    System.out.println("=== BOOKMARK 조회 시작 ===");
                                    System.out.println("User ID: " + users.getUserId());
                                }

                                // 1. Draft 필터
                                AggregationOperation matchDraft = Aggregation.match(
                                        Criteria.where("draft").ne(true)
                                );

                                // 2. Lookup - 이제 타입 변환 없이 직접 사용 가능
                                AggregationOperation lookupBookmarks = Aggregation.lookup(
                                        "con_bookmarks_users_documents",
                                        "_id",  // ObjectId 타입 그대로 사용
                                        "documentId",  // Bookmark의 documentId도 ObjectId 타입
                                        "bookmarks"
                                );

                                // 3. Match bookmarks - 현재 사용자가 북마크한 문서만 필터
                                AggregationOperation matchUserBookmarks = Aggregation.match(
                                        Criteria.where("bookmarks").elemMatch(
                                                Criteria.where("userId").is(users.getUserId())
                                                        .and("isBookmarked").is(true)
                                        )
                                );

                                // 4. Sort, Skip, Limit
                                AggregationOperation sortOperation = Aggregation.sort(sort);
                                AggregationOperation skipOperation = Aggregation.skip(
                                        (long) pageable.getPageNumber() * pageable.getPageSize()
                                );
                                AggregationOperation limitOperation = Aggregation.limit(pageable.getPageSize());

                                // 5. 전체 aggregation (addFields 제거)
                                Aggregation aggregation = Aggregation.newAggregation(
                                        matchDraft,
                                        lookupBookmarks,
                                        matchUserBookmarks,
                                        sortOperation,
                                        skipOperation,
                                        limitOperation
                                );

                                // 6. Count aggregation (addFields 제거)
                                Aggregation countAggregation = Aggregation.newAggregation(
                                        matchDraft,
                                        lookupBookmarks,
                                        matchUserBookmarks,
                                        Aggregation.count().as("total")
                                );

                                // 7. 실행
                                AggregationResults<Documents> results = mongoTemplate.aggregate(
                                        aggregation, "documents", Documents.class
                                );

                                if (debugMode) {
                                    System.out.println("최종 결과 개수: " + results.getMappedResults().size());
                                }

                                // 8. 카운트 수행
                                CountResult countResult = mongoTemplate.aggregate(
                                        countAggregation, "documents", CountResult.class
                                ).getUniqueMappedResult();

                                long totalCount = countResult != null ? countResult.getTotal() : 0;

                                documents = PageableExecutionUtils.getPage(
                                        results.getMappedResults(),
                                        pageable,
                                        () -> totalCount
                                );
                            } else if (BlogConstants.BlogPageType.DRAFT.getValue().equals(pageDTO.getPageType())) {
                                documents = this.findByCreatedUserOrUpdatedUserAndDraft(users.getUserId(), users.getUserId(), pageable, true);
                            }
                        } else {
                            throw new Exception("there is no user : " + username);
                        }
                    } else {
                        throw new Exception("there is no login information");
                    }
                }
            }
            List<DocumentsDTO> documentsDTO = documents.getContent().stream().map(document -> {
                DocumentsDTO documentDTO = new DocumentsDTO();
                BeanUtils.copyProperties(document, documentDTO);
                return documentDTO;
            }).collect(Collectors.toList());

            DocumentsInfo documentsInfo = new DocumentsInfo();
            documentsInfo.setDocuments(documentsDTO);
            documentsInfo.setTotalPagesCnt(documents.getTotalPages());
            documentsInfo.setTotalDocsCnt(documents.getTotalElements());
            return documentsInfo;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public DocumentsInfo searchDocuments(String value, PaginationDTO pageDTO) throws Exception {
        try {
            Sort.Order updatedSort = "ASC".equals(pageDTO.getDateSort()) ? Sort.Order.asc("updated") : Sort.Order.desc("updated");
            Sort sort = Sort.by(updatedSort);
            Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);

            Page<Documents> documents = blogsRepo.findByTitleOrContentsWithDisclose(value, value, pageable);

            List<DocumentsDTO> documentsDTO = new ArrayList<>();
            DocumentsInfo documentsInfo = new DocumentsInfo();

            if (!ObjectUtils.isEmpty(documents.getContent())) {
                documents.getContent().stream().forEach(content -> {
                    DocumentsDTO documentDTO = new DocumentsDTO();
                    BeanUtils.copyProperties(content, documentDTO);
                    documentsDTO.add(documentDTO);
                });
                documentsInfo.setDocuments(documentsDTO);
                documentsInfo.setTotalDocsCnt(documents.getTotalElements());
                documentsInfo.setTotalPagesCnt(documents.getTotalPages());
            }
            return documentsInfo;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public DocumentsDTO createAfterSaveDocument(DocumentsDTO newData) {
        LocalDateTime now = LocalDateTime.now();
        Documents documents = blogsRepo.findById(newData.getId()).get();

        documents.setContents(newData.getContents());
        documents.setThumbnailImgUrl(newData.getThumbnailImgUrl());

        Documents newDocument = blogsRepo.save(documents);
        DocumentsDTO newDocumentDTO = new DocumentsDTO();
        BeanUtils.copyProperties(newDocument, newDocumentDTO);

        return newDocumentDTO;
    }

    @Override
    public DocumentsDTO getDocument(String id) throws Exception {
        try {
            Documents documents = blogsRepo.findById(id).get();
            DocumentsDTO documentsDTO = new DocumentsDTO();
            BeanUtils.copyProperties(documents, documentsDTO);
            return documentsDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void deleteDocument(String id) throws Exception {
        try {
            blogsRepo.deleteById(id);
        } catch (Exception e) {
            throw e;
        }
    }

    // 내부적으로 카운트를 담을 DTO
    private static class CountResult {
        private long total;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }

    private Page<Documents> findByCreatedUserOrUpdatedUserAndDraft(String createdUser, String updatedUser, Pageable pageable, boolean isDraft) {
        // 조건 생성
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("createdUser").is(createdUser),
                Criteria.where("updatedUser").is(updatedUser)
        ).and("draft").is(isDraft);

        // 쿼리 생성
        Query query = new Query(criteria);

        // 페이지네이션 적용
        long total = mongoTemplate.count(query, Documents.class);  // 전체 데이터 개수 계산
        query.with(pageable);  // Pageable로 페이징 정보 적용

        // 데이터 조회
        List<Documents> entities = mongoTemplate.find(query, Documents.class);

        // Page 객체로 반환 (페이지네이션 정보와 결과 리스트)
        return new PageImpl<>(entities, pageable, total);
    };

    @Override
    public DocumentsDTO setAsFeatured(String id, Documents.FeaturedInfo featuredInfo,
                                      LocalDateTime startDate, LocalDateTime endDate,
                                      String approvedBy) {
        try {
            Documents document = blogsRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

            // Draft 상태인 글은 Featured로 설정 불가
            if (document.isDraft()) {
                throw new RuntimeException("임시저장 상태의 글은 Featured로 설정할 수 없습니다");
            }

            // 동일 기간에 이미 활성화된 Featured 확인
            List<Documents> conflictingDocs = blogsRepo
                    .findFeaturedDocumentsByDateRange(startDate, endDate);

            if (!conflictingDocs.isEmpty() && !conflictingDocs.get(0).getId().equals(id)) {
                throw new RuntimeException("해당 기간에 이미 Featured 글이 존재합니다");
            }

            // Featured 정보 설정
            if (featuredInfo == null) {
                // 기본 Featured 정보 생성
                featuredInfo = Documents.FeaturedInfo.builder()
                        .featuredTitle(document.getTitle())
                        .featuredSubtitle("Explore Korea with Koraveler")
                        .featuredImageUrl(document.getThumbnailImgUrl())
                        .displayPriority(1)
                        .build();
            }

            // Featured 스케줄 설정
            Documents.FeaturedSchedule schedule = Documents.FeaturedSchedule.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .isActive(true)
                    .approvedBy(approvedBy)
                    .approvedAt(LocalDateTime.now())
                    .build();

            document.setFeaturedReady(true);
            document.setFeaturedInfo(featuredInfo);
            document.setFeaturedSchedule(schedule);
            document.setUpdated(LocalDateTime.now());
            document.setUpdatedUser(approvedBy);

            Documents savedDocument = blogsRepo.save(document);

            DocumentsDTO resultDTO = new DocumentsDTO();
            BeanUtils.copyProperties(savedDocument, resultDTO);

            log.info("문서를 Featured로 설정: {} (기간: {} ~ {})", id, startDate, endDate);
            return resultDTO;

        } catch (Exception e) {
            log.error("Featured 설정 실패: {}", id, e);
            throw new RuntimeException("Featured 설정 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public DocumentsDTO removeFromFeatured(String id) {
        try {
            Documents document = blogsRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

            // Featured 관련 정보 모두 제거
            document.setFeaturedReady(false);
            document.setFeaturedInfo(null);
            document.setFeaturedSchedule(null);
            document.setUpdated(LocalDateTime.now());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                document.setUpdatedUser(auth.getName());
            }

            Documents savedDocument = blogsRepo.save(document);

            DocumentsDTO resultDTO = new DocumentsDTO();
            BeanUtils.copyProperties(savedDocument, resultDTO);

            log.info("Featured 설정 해제: {}", id);
            return resultDTO;

        } catch (Exception e) {
            log.error("Featured 해제 실패: {}", id, e);
            throw new RuntimeException("Featured 해제 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public DocumentsDTO updateFeaturedInfo(String id, Documents.FeaturedInfo featuredInfo) {
        try {
            Documents document = blogsRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

            if (!document.isFeaturedReady()) {
                throw new RuntimeException("Featured로 설정되지 않은 문서입니다");
            }

            document.setFeaturedInfo(featuredInfo);
            document.setUpdated(LocalDateTime.now());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                document.setUpdatedUser(auth.getName());
            }

            Documents savedDocument = blogsRepo.save(document);

            DocumentsDTO resultDTO = new DocumentsDTO();
            BeanUtils.copyProperties(savedDocument, resultDTO);

            log.info("Featured 정보 수정: {}", id);
            return resultDTO;

        } catch (Exception e) {
            log.error("Featured 정보 수정 실패: {}", id, e);
            throw new RuntimeException("Featured 정보 수정 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DocumentsDTO> getActiveFeaturedDocuments(int limit) {
        LocalDateTime now = LocalDateTime.now();

        // 우선순위 순으로 정렬하여 활성화된 Featured 문서 조회
        Sort sort = Sort.by(Sort.Direction.ASC, "featuredInfo.displayPriority");
        List<Documents> activeDocs = blogsRepo
                .findActiveFeaturedDocumentsOrderByPriority(now, sort);

        // limit 적용
        return activeDocs.stream()
                .limit(limit)
                .map(doc -> {
                    DocumentsDTO dto = new DocumentsDTO();
                    BeanUtils.copyProperties(doc, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public DocumentsInfo getFeaturableDocuments(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "updated"));

        Page<Documents> documents;

        if (StringUtils.hasText(search)) {
            // 검색어가 있으면 제목/내용에서 검색
            documents = blogsRepo.findByDraftFalseAndFeaturedReadyFalseAndTitleContainingOrContentsContaining(
                    search, search, pageable);
        } else {
            // Featured가 아니고 draft가 아닌 모든 글
            documents = blogsRepo.findByDraftFalseAndFeaturedReadyFalse(pageable);
        }

        List<DocumentsDTO> dtoList = documents.getContent().stream()
                .map(doc -> {
                    DocumentsDTO dto = new DocumentsDTO();
                    BeanUtils.copyProperties(doc, dto);
                    return dto;
                })
                .collect(Collectors.toList());

        DocumentsInfo info = new DocumentsInfo();
        info.setDocuments(dtoList);
        info.setTotalDocsCnt(documents.getTotalElements());
        info.setTotalPagesCnt(documents.getTotalPages());

        return info;
    }

    @Override
    public DocumentsInfo getFeaturedHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "featuredSchedule.approvedAt"));

        // Featured로 설정된 적이 있는 모든 문서 (활성/비활성 포함)
        Page<Documents> featuredDocs = blogsRepo.findByFeaturedReadyTrue(pageable);

        List<DocumentsDTO> dtoList = featuredDocs.getContent().stream()
                .map(doc -> {
                    DocumentsDTO dto = new DocumentsDTO();
                    BeanUtils.copyProperties(doc, dto);
                    return dto;
                })
                .collect(Collectors.toList());

        DocumentsInfo info = new DocumentsInfo();
        info.setDocuments(dtoList);
        info.setTotalDocsCnt(featuredDocs.getTotalElements());
        info.setTotalPagesCnt(featuredDocs.getTotalPages());

        return info;
    }
}


