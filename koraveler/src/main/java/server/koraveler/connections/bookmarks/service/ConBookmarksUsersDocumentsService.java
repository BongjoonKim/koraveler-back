package server.koraveler.connections.bookmarks.service;

import server.koraveler.blog.dto.DocumentsInfo;
import server.koraveler.connections.bookmarks.dto.BookmarkDTO;

import java.util.List;

public interface ConBookmarksUsersDocumentsService {
    BookmarkDTO createBookmark (BookmarkDTO bookmarkDTO) throws Exception;

    boolean isBookmarked (String documentId) throws Exception;
    List<DocumentsInfo.DocumentsDTO> getDocuments() throws Exception;
    void deleteBookmarkByUsers (String userId) throws Exception;
    void deleteBookmarkByDocumentsId (String documentId) throws Exception;


}
