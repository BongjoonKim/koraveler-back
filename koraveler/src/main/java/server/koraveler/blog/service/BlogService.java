package server.koraveler.blog.service;

import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.blog.dto.PaginationDTO;
import server.koraveler.blog.model.Documents;

public interface BlogService {
    // 생성 후 저장
    DocumentsInfo.DocumentsDTO createDocument(DocumentsInfo.DocumentsDTO documentsDTO);
    // 생성 후 저장
    DocumentsInfo.DocumentsDTO createAfterSaveDocument(DocumentsInfo.DocumentsDTO documentsDTO);
    DocumentsInfo.DocumentsDTO saveDocument(DocumentsInfo.DocumentsDTO documentsDTO);

    DocumentsInfo getDocuments(PaginationDTO pageDTO) throws Exception;
    DocumentsInfo.DocumentsDTO getDocument(String id) throws Exception;
    void deleteDocument(String id) throws Exception;

    DocumentsInfo.DocumentsDTO searchDocuments(String value);

}
