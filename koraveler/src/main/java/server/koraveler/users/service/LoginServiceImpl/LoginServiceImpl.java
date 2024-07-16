package server.koraveler.users.service.LoginServiceImpl;

import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.NullableUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import server.koraveler.error.CustomException;
import server.koraveler.users.dto.CustomUserDetails;
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

    // refresh token verify, access token 재생성
    @Override
    public TokenDTO refreshToken(String refreshToken) throws CustomException, Exception {
        try {
            TokenDTO tokenDTO = new TokenDTO();
            Claims claims = JwtUtil.verifyToken(refreshToken);
            System.out.println("claims = " + claims.getSubject().isEmpty());
            if(!claims.getSubject().isEmpty()) {
                String userId = claims.getSubject();
                String newAccessToken  = JwtUtil.generateAccessToken(userId);


                tokenDTO.setRefreshToken(refreshToken);
                tokenDTO.setAccessToken(newAccessToken);
                return tokenDTO;
            }
        } catch (CustomException e) {
            String message = e.getMessage();
            if (message.contains("Token expired")) {
                throw e;
            } else {
                // 잘못된 리프레시 토큰인 경우
                throw e;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            return null;
        }
    }

    // 로그인한 사용자 정보
    @Override
    public UsersDTO loginUser() throws Exception {
        try {
           Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("authentication = " + authentication);
            if (authentication != null && authentication.getPrincipal() != null) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                System.out.println("userDetails = " + userDetails);
                String username = userDetails.getUsername();

                Users users = usersRepo.findByUserId(username);
                if (users != null) {
                    UsersDTO usersDTO = new UsersDTO();
                    BeanUtils.copyProperties(users, usersDTO);
                    return usersDTO;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void logout() throws Exception {
        try {
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            throw e;
        }
    }
}
