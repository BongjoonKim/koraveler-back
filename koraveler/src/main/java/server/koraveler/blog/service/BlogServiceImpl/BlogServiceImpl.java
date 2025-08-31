package server.koraveler.blog.service.BlogServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
import server.koraveler.blog.constants.BlogConstants;
import server.koraveler.blog.dto.DocumentsDTO;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.model.Documents;
import server.koraveler.blog.repo.BlogsRepo;
import server.koraveler.blog.service.BlogService;
import server.koraveler.connections.bookmarks.repo.BookmarksRepo;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BlogsRepo blogsRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private BookmarksRepo bookmarksRepo;

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
                                // 1. documents 컬렉션에서 createdUser 또는 updatedUser가 userId인 문서 찾기
                                AggregationOperation matchDocuments = Aggregation.match(
                                        new Criteria().orOperator(
                                                Criteria.where("createdUser").is(users.getUserId()),
                                                Criteria.where("updatedUser").is(users.getUserId())
                                        ).and("draft").is(false)
                                );

                                // 2. con_bookmarks_users_documents 컬렉션에서 userId가 userId인 문서 찾기
                                AggregationOperation lookupBookmarks = Aggregation.lookup(
                                        "con_bookmarks_users_documents",   // 외부 컬렉션 이름
                                        "_id",                             // documents 컬렉션의 _id
                                        "documentId",                      // con_bookmarks_users_documents의 documentId
                                        "bookmarks"                        // 결과를 저장할 필드 이름
                                );

                                // 3. bookmark가 userId와 일치하는 필드만 필터링
                                AggregationOperation matchBookmarks = Aggregation.match(
                                        Criteria.where("bookmarks.userId").is(users.getUserId())
                                );

                                // 4. Aggregation 조합
                                Aggregation aggregation = Aggregation.newAggregation(
                                        matchDocuments,
                                        lookupBookmarks,
                                        matchBookmarks
                                );

                                // 5. 결과 실행 및 반환
                                AggregationResults<Documents> results = mongoTemplate.aggregate(
                                        aggregation, "documents", Documents.class
                                );


                                Aggregation countAggregation = Aggregation.newAggregation(
                                        matchDocuments,
                                        lookupBookmarks,
                                        matchBookmarks,
                                        Aggregation.count().as("total")
                                );

                                // 7. 카운트 수행
                                long totalCount = mongoTemplate.aggregate(countAggregation, "documents", CountResult.class)
                                        .getUniqueMappedResult() != null ? mongoTemplate.aggregate(countAggregation, "documents", CountResult.class)
                                        .getUniqueMappedResult().getTotal() : 0;

                                documents = PageableExecutionUtils.getPage(
                                        results.getMappedResults(),
                                        pageable,
                                        () -> totalCount);
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

            Page<Documents> documents = blogsRepo.findAllByTitleContainingIgnoreCaseOrContentsContainingIgnoreCase(value, value, pageable);

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

//    @Override
//    public DocumentsInfo.DocumentsDTO searchDocuments(String value) {
//        org.springframework.data.elasticsearch.core.query.Criteria criteria = new org.springframework.data.elasticsearch.core.query.Criteria("title").contains(value)
//                .or("contents").contains(value);
//
//        CriteriaQuery query = new CriteriaQuery(criteria);
//
//        org.springframework.data.elasticsearch.core.SearchHits<Documents> searchHits = elasticsearchTemplate.search(query, Documents.class);
//        List<Documents> documents = searchHits.stream().map(hit -> hit.getContent()).collect(Collectors.toList());
//
//        DocumentsInfo.DocumentsDTO newDocument = new DocumentsInfo.DocumentsDTO();
//        BeanUtils.copyProperties(documents, newDocument);
//
//        return newDocument;
//    }
}


