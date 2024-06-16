package server.koraveler.users.service.LoginServiceImpl;

import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.NullableUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import server.koraveler.users.dto.TokenDTO;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.model.Users;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.LoginService;
import server.koraveler.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UsersDTO login(UsersDTO usersDTO) {
        // 사용자 비번 암호화 후 저장
        Users users = findUserByUserId(usersDTO.getUserId());

        if (ObjectUtils.isEmpty(users)) {
            createUser(usersDTO);
        }
        return null;
    }

    private Users findUserByUserId(String userId) {
        return usersRepo.findByUserId(userId);
    }

    private UsersDTO createUser(UsersDTO usersDTO) {
        LocalDateTime now = LocalDateTime.now();
        Users users = new Users();

        BeanUtils.copyProperties(usersDTO, users);

        users.setCreated(now);
        users.setUpdated(now);
        users.setEnabled(true);

        try {
            usersRepo.save(users);
        } catch (Exception e) {
            throw e;
        }

        UsersDTO newUsersDTO = new UsersDTO();
        BeanUtils.copyProperties(users,newUsersDTO);
        return newUsersDTO;
    }


    @Override
    public TokenDTO refreshToken(String refreshToken) {
        boolean verifyRefreshToken = false;
        try {
            Claims claims = JwtUtil.verifyToken(refreshToken);

            if(claims.getSubject().isEmpty()) {
                String userId = claims.getSubject();
                String newAccessToken  = JwtUtil.generateAccessToken(userId);

                TokenDTO tokenDTO = new TokenDTO();
                tokenDTO.setRefreshToken(refreshToken);
                return tokenDTO;
            }
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("Token expired")) {
                // 리프레시 토큰이 만료된 경우
                Claims claims = JwtUtil.verifyToken(refreshToken);
                String username = claims.getSubject();
                String newAccessToken = JwtUtil.generateAccessToken(username);
                String newRefreshToken = JwtUtil.generateRefreshToken(username);

//                return TokenRefreshResponse(newAccessToken, newRefreshToken));
            } else {
                // 잘못된 리프레시 토큰인 경우
//                return ResponseEntity.status(401).body("Invalid refresh token");
            }
        }
    }
}
