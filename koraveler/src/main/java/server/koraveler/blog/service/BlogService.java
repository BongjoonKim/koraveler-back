package server.koraveler.blog.service;

import server.koraveler.blog.dto.DocumentsDTO;
import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.model.Documents;

public interface BlogService {
    // 생성 후 저장
    DocumentsDTO createDocument(DocumentsDTO documentsDTO);
    // 생성 후 저장
    DocumentsDTO createAfterSaveDocument(DocumentsDTO documentsDTO);
    DocumentsDTO saveDocument(DocumentsDTO documentsDTO);

    DocumentsInfo getDocuments(PaginationDTO pageDTO) throws Exception;
    DocumentsInfo searchDocuments(String value, PaginationDTO pageDTO) throws Exception;
    DocumentsDTO getDocument(String id) throws Exception;
    void deleteDocument(String id) throws Exception;
}
