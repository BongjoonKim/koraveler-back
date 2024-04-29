package server.koraveler.user.service.UserServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.koraveler.user.model.User;
import server.koraveler.user.repo.UserRepo;
import server.koraveler.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepo userRepo;

    @Override
    public User createUser(User user) {
        return userRepo.insert(user);
    }
}
