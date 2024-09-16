package server.koraveler.users.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import server.koraveler.users.model.Users;

import java.util.List;

@Repository
public interface UsersRepo extends MongoRepository<Users, String> {
    Users findByEmail(String email);
    Users findByUserId(String userId);
}
