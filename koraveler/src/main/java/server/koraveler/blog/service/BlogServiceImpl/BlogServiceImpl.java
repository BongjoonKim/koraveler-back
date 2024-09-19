package server.koraveler.blog.service.BlogServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import server.koraveler.blog.constants.BlogConstants;
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

import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

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
    public DocumentsInfo.DocumentsDTO createDocument(DocumentsInfo.DocumentsDTO documentsDTO) {

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
            Documents afterDocument = blogsRepo.save(documents);

            DocumentsInfo.DocumentsDTO newDocDTO = new DocumentsInfo.DocumentsDTO();
            BeanUtils.copyProperties(afterDocument, newDocDTO);

            return newDocDTO;
        }
        return null;
    }

    @Override
    public DocumentsInfo.DocumentsDTO saveDocument(DocumentsInfo.DocumentsDTO documentsDTO) {
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
                Documents afterDocument = blogsRepo.save(documents);

                DocumentsInfo.DocumentsDTO newDocDTO = new DocumentsInfo.DocumentsDTO();
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
            if ("all".equals(pageDTO.getFolderId()) || pageDTO.getPage() == -1) {
                documents = blogsRepo.findAll(PageRequest.of(pageDTO.getPage(), pageDTO.getSize()));
            } else {
                if (ObjectUtils.isEmpty(pageDTO.getPageType())) {
                    documents = blogsRepo.findAll(PageRequest.of(pageDTO.getPage(), pageDTO.getSize()));
                } else {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.getPrincipal() != null) {
                        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                        String username = userDetails.getUsername();
                        Users users = usersRepo.findByUserId(username);
                        if (!ObjectUtils.isEmpty(users)) {
                            if (BlogConstants.BlogPageType.MY_BLOG.getValue().equals(pageDTO.getPageType())) {
                                Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize());
                                documents = blogsRepo.findAllByCreatedUserOrUpdatedUser(users.getUserId(), users.getUserId(), pageable);
                            } else if (BlogConstants.BlogPageType.BOOKMARK.getValue().equals(pageDTO.getPageType())) {
                                // 1. documents 컬렉션에서 createdUser 또는 updatedUser가 userId인 문서 찾기
                                AggregationOperation matchDocuments = Aggregation.match(
                                        new Criteria().orOperator(
                                                Criteria.where("createdUser").is(users.getUserId()),
                                                Criteria.where("updatedUser").is(users.getUserId())
                                        )
                                );

                                // 타입이 다를 경우 사용
//                                AggregationOperation projectIdToString = project()
//                                        .andExpression("toString(_id)").as("idAsString")
//                                        .andExclude("_id"); // 필요한 필드 포함

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

                                List<Documents> results22 = results.getMappedResults();
                                System.out.println("results22 = " + results22);












                                Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize());


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
                            }
                        } else {
                            throw new Exception("there is no user : " + username);
                        }
                    } else {
                        throw new Exception("there is no login information");
                    }
                }
            }
            List<DocumentsInfo.DocumentsDTO> documentsDTO = documents.getContent().stream().map(document -> {
                DocumentsInfo.DocumentsDTO documentDTO = new DocumentsInfo.DocumentsDTO();
                BeanUtils.copyProperties(document, documentDTO);
                return documentDTO;
            }).collect(Collectors.toList());

            DocumentsInfo documentsInfo = new DocumentsInfo();
            documentsInfo.setDocumentsDTO(documentsDTO);
            documentsInfo.setTotalPagesCnt(documents.getTotalPages());
            documentsInfo.setTotalDocsCnt(documents.getTotalElements());
            return documentsInfo;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public DocumentsInfo.DocumentsDTO createAfterSaveDocument(DocumentsInfo.DocumentsDTO newData) {
        LocalDateTime now = LocalDateTime.now();
        Documents documents = blogsRepo.findById(newData.getId()).get();

        documents.setContents(newData.getContents());
        documents.setThumbnailImgUrl(newData.getThumbnailImgUrl());

        Documents newDocument = blogsRepo.save(documents);
        DocumentsInfo.DocumentsDTO newDocumentDTO = new DocumentsInfo.DocumentsDTO();
        BeanUtils.copyProperties(newDocument, newDocumentDTO);

        return newDocumentDTO;
    }

    @Override
    public DocumentsInfo.DocumentsDTO getDocument(String id) throws Exception {
        try {
            Documents documents = blogsRepo.findById(id).get();
            DocumentsInfo.DocumentsDTO documentsDTO = new DocumentsInfo.DocumentsDTO();
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
}
