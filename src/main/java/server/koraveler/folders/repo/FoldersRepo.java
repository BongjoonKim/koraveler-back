package server.koraveler.folders.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import server.koraveler.folders.model.Folders;
import java.util.List;

@Repository
public interface FoldersRepo extends MongoRepository<Folders, String> {

    // 부모 ID로 폴더 찾기
    List<Folders> findByParentId(String parentId);

    // 경로 패턴으로 하위 폴더 찾기
    @Query("{ 'path': { $regex: ?0 } }")
    List<Folders> findByPathRegex(String pathRegex);

    // 사용자 ID와 부모 ID로 폴더 찾기
    List<Folders> findByUserIdAndParentId(String userId, String parentId);
}
