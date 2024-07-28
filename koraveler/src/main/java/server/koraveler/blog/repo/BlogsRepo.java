package server.koraveler.blog.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.blog.model.Documents;

@Repository
public interface BlogsRepo extends MongoRepository<Documents, String> {

}