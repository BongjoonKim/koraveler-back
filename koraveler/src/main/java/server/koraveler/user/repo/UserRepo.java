package server.koraveler.user.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.user.model.User;

@Repository
public interface UserRepo extends MongoRepository<User, String> {
    User findByEmail(String email);
}
