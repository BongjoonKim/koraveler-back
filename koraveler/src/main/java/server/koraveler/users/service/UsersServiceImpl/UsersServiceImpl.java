package server.koraveler.users.service.UsersServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.UsersService;

import java.util.HashMap;
import java.util.Map;

@Service
public class UsersServiceImpl implements UsersService {

//    @Autowired
//    MongoTemplate mongoTemplate;
    @Autowired
    private UsersRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UsersDTO createUser(Users user) throws Exception {
        try {
            user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
            Users users = userRepo.save(user);
            UsersDTO usersDTO = new UsersDTO();
            BeanUtils.copyProperties(users, usersDTO);
            return usersDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public UsersDTO getUser(String email) throws Exception {
        try {
            Users users = userRepo.findByEmail(email).get(0);
            UsersDTO usersDTO = new UsersDTO();
            BeanUtils.copyProperties(users, usersDTO);
            return usersDTO;
        } catch (Exception e) {
            throw e;
        }
    }
}
