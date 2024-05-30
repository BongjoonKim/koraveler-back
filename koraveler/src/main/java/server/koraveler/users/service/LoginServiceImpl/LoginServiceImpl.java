package server.koraveler.users.service.LoginServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.LoginService;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UsersRepo userRepo;
    @Override
    public UsersDTO login(UsersDTO usersDTO) {
        // 사용자 비번 암호화 후 저장
    }

    private Users findUserByUserId(String userId) {
        return userRepo.findByUserId(userId).get(0);
    }

    private UsersDTO findOrCreateUser(UsersDTO usersDTO) {
        // 새로운 사용자 생성
        if (findUserByUserId(usersDTO.getUserId()) == null) {
            Users newUsers = new Users();
            BeanUtils.copyProperties(usersDTO, newUsers);

            
        } else {    // 기존 사용자 불러오기

        }
    }
}
