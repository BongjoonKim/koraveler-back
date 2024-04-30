package server.koraveler.users.service.UsersServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.UsersService;

import java.util.HashMap;
import java.util.Map;

@Service
public class UsersServiceImpl implements UsersService {

    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    private UsersRepo userRepo;

    @Override
    public Users createUser(Users user) {
        try {
            Users newUser = new Users();
            newUser.setTitle("dddddd");

            Map<String, String> data = new HashMap<>();
//            data.put("title", "케케");
            userRepo.save(newUser);
            return null;
//            return userRepo.insert(newUser);
        } catch (Exception e) {
            throw e;
        }
    }
}
