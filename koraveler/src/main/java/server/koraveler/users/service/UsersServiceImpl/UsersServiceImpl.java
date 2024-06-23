package server.koraveler.users.service.UsersServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import server.koraveler.users.dto.CustomUserDetails;
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

    @Override
    public UsersDTO getUserFromAccessToken() throws Exception {
        try {
            UsersDTO usersDTO = new UsersDTO();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            if (principal != null && (principal instanceof UserDetails)) {
                UserDetails userData = (UserDetails) principal;
                if (!ObjectUtils.isEmpty(userData.getUsername())) {
                    Users users = userRepo.findByUserId(userData.getUsername());
                    System.out.println("users = " + users);

                    BeanUtils.copyProperties(users, usersDTO);
                }
                return usersDTO;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw e;
        }
    }


}
