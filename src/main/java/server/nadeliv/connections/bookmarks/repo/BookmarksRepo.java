package server.nadeliv.connections.bookmarks.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.nadeliv.connections.bookmarks.model.Bookmark;

@Repository
public interface BookmarksRepo extends MongoRepository<Bookmark, String> {
    Bookmark findByDocumentIdAndUserId(String documentId, String userId);
}
