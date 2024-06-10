package server.koraveler.users.service.CustomUserDetailsServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import server.koraveler.users.dto.CustomUserDetails;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;

import java.util.List;

@Service
public class CustomUserDetailService implements UserDetailsService {
    @Autowired
    private UsersRepo usersRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users users = usersRepo.findByUserId(username);
        if (users == null) {
            throw new UsernameNotFoundException("User not found");
        }
        users.setUserPassword(users.getUserPassword());

        return new CustomUserDetails(users);
    }
}
