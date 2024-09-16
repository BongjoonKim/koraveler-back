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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Users findUserByUserId(String userId) {
        return usersRepo.findByUserId(userId);
    }


    @Override
    public UsersDTO createUser(UsersDTO usersDTO) throws Exception {
        try {
            // validation 체크
            // 1. 이미 있는 아이디인 경우
            Users users = usersRepo.findByUserId(usersDTO.getUserId());
            if (!ObjectUtils.isEmpty(users)) {
                throw new Exception("이미 존재하는 ID입니다");
            }

            // 2. 이미 있는 이메일인 경우
            users = usersRepo.findByEmail(usersDTO.getEmail());
            if (!ObjectUtils.isEmpty(users)) {
                throw new Exception("이미 존재하는 이메일입니다");
            }

            LocalDateTime now = LocalDateTime.now();
            users = new Users();

            BeanUtils.copyProperties(usersDTO, users);
            users.setUserPassword(passwordEncoder.encode(usersDTO.getUserPassword()));
            users.setCreated(now);
            users.setUpdated(now);
            users.setEnabled(true);
            users.setRoles(Collections.singletonList("user"));

            usersRepo.save(users);

            UsersDTO newUsersDTO = new UsersDTO();
            BeanUtils.copyProperties(users,newUsersDTO);
            return newUsersDTO;
        } catch (Exception e) {
            throw e;
        }

    }

    // refresh token verify, access token 재생성
    @Override
    public TokenDTO refreshToken(String refreshToken) throws CustomException, Exception {
        try {
            TokenDTO newTokenDTO = new TokenDTO();
            Claims claims = JwtUtil.verifyToken(refreshToken);
            System.out.println("claims = " + claims.getSubject().isEmpty());
            if(!claims.getSubject().isEmpty()) {
                String userId = claims.getSubject();
                String newAccessToken  = JwtUtil.generateAccessToken(userId);
                newTokenDTO.setRefreshToken(refreshToken);
                newTokenDTO.setAccessToken(newAccessToken);
            }
            return newTokenDTO;
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
