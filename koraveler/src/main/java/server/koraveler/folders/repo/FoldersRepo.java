package server.koraveler.folders.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import server.koraveler.folders.model.Folders;

import java.util.List;

public interface FoldersRepo extends MongoRepository<Folders, String> {

    // 부모 ID로 폴더 찾기
    List<Folders> findByParentId(String parentId);

    // 사용자 ID로 폴더 찾기
    List<Folders> findByUserId(String userId);

    // 경로로 폴더 찾기
    Folders findByPath(String path);

    // 경로 패턴으로 하위 폴더 찾기
    @Query("{ 'path': { $regex: ?0 } }")
    List<Folders> findByPathRegex(String pathRegex);

    // 사용자 ID와 부모 ID로 폴더 찾기
    List<Folders> findByUserIdAndParentId(String userId, String parentId);

    // 공개된 폴더만 찾기
    List<Folders> findByIsPublicTrue();
}
