package server.koraveler.blog.service;

import server.koraveler.blog.dto.DocumentsInfo;

public interface BlogService {
    DocumentsInfo.DocumentsDTO createDocument(DocumentsInfo.DocumentsDTO documentsDTO);
}
