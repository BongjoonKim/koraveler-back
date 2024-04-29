package server.koraveler.user.service;

import org.springframework.stereotype.Service;
import server.koraveler.user.model.User;

public interface UserService {
    public User createUser(User user);
}
