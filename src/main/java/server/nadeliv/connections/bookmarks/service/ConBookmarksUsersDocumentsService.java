package server.nadeliv.connections.bookmarks.service;

import server.nadeliv.blog.dto.DocumentsDTO;
import server.nadeliv.blog.dto.DocumentsInfo;
import server.nadeliv.connections.bookmarks.dto.BookmarkDTO;

import java.util.List;

public interface ConBookmarksUsersDocumentsService {
    BookmarkDTO createBookmark (BookmarkDTO bookmarkDTO) throws Exception;

    boolean isBookmarked (String documentId) throws Exception;
    List<DocumentsDTO> getDocuments() throws Exception;
    void deleteBookmarkByUsers (String userId) throws Exception;
    void deleteBookmarkByDocumentsId (String documentId) throws Exception;


}
