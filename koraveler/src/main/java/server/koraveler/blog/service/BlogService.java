package server.koraveler.blog.service;

import server.koraveler.blog.dto.DocumentsDTO;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.model.Documents;

import java.time.LocalDateTime;
import java.util.List;

public interface BlogService {
    // ========== 기존 블로그 메서드 ==========
    DocumentsDTO createDocument(DocumentsDTO documentsDTO);
    DocumentsDTO createAfterSaveDocument(DocumentsDTO documentsDTO);
    DocumentsDTO saveDocument(DocumentsDTO documentsDTO);

    DocumentsInfo getDocuments(PaginationDTO pageDTO) throws Exception;
    DocumentsInfo searchDocuments(String value, PaginationDTO pageDTO) throws Exception;
    DocumentsDTO getDocument(String id) throws Exception;
    void deleteDocument(String id) throws Exception;

    // ========== Featured 관리 메서드 ==========
    // Featured로 설정
    DocumentsDTO setAsFeatured(String id, Documents.FeaturedInfo featuredInfo,
                               LocalDateTime startDate, LocalDateTime endDate,
                               String approvedBy);

    // Featured 해제
    DocumentsDTO removeFromFeatured(String id);

    // Featured 정보만 수정
    DocumentsDTO updateFeaturedInfo(String id, Documents.FeaturedInfo featuredInfo);

    // 현재 활성화된 Featured 글 조회
    List<DocumentsDTO> getActiveFeaturedDocuments(int limit);

    // Featured 가능한 일반 글 목록
    DocumentsInfo getFeaturableDocuments(int page, int size, String search);

    // Featured 히스토리
    DocumentsInfo getFeaturedHistory(int page, int size);
}