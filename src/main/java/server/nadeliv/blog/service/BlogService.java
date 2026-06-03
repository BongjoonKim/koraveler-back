package server.nadeliv.blog.service;

import server.nadeliv.blog.dto.DocumentsDTO;
import server.nadeliv.blog.dto.DocumentsInfo;
import server.nadeliv.blog.dto.PaginationDTO;
import server.nadeliv.blog.dto.PopularPostDTO;
import server.nadeliv.blog.model.Documents;

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
    void restoreDocument(String id) throws Exception;

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

    // ========== Popular Posts (사이드바 위젯) ==========
    // 기간 내 조회수 기준 인기 글 Top N
    // @param period "day" | "week" | "month" | "all"
    // @param limit 반환 개수 (예: 3)
    List<PopularPostDTO> getPopularPosts(String period, int limit);

    // ========== Following Feed ==========
    // viewerUserId 가 팔로우 중인 사용자들이 작성한 발행 글 페이지네이션
    DocumentsInfo getFollowingFeed(String viewerUserId, PaginationDTO pageDTO);

    // viewerUserId 가 팔로우 중인 사용자의 글 중, since 시점 이후 발행/수정된 글 수.
    // 사이드바 Following 탭 뱃지에 사용.
    long countFollowingFeedSince(String viewerUserId, LocalDateTime since);
}