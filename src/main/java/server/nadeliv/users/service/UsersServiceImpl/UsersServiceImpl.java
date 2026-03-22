package server.nadeliv.users.service.UsersServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import server.nadeliv.error.CustomException;
import server.nadeliv.error.ErrorCode;
import server.nadeliv.users.dto.CustomUserDetails;
import server.nadeliv.users.dto.UsersDTO;
import server.nadeliv.users.dto.request.PasswordChangeRequest;
import server.nadeliv.users.dto.request.UserDeleteRequest;
import server.nadeliv.users.dto.request.UserUpdateRequest;
import server.nadeliv.users.dto.response.UserProfileResponse;
import server.nadeliv.users.model.Users;
import server.nadeliv.users.repo.UsersRepo;
import server.nadeliv.users.service.UsersService;

import java.time.LocalDateTime;

@Service
public class UsersServiceImpl implements UsersService {

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
            Users users = userRepo.findByEmail(email);
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

    @Override
    public UsersDTO updateUser(Users user) throws Exception {
        try {
            Users updatedUser = userRepo.save(user);
            UsersDTO usersDTO = new UsersDTO();
            BeanUtils.copyProperties(updatedUser, usersDTO);
            return usersDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public UserProfileResponse getUserProfile(String userId) {
        Users user = userRepo.findByUserId(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return toProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateUserProfile(String userId, UserUpdateRequest request) {
        Users user = userRepo.findByUserId(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 이메일 변경 시 중복 검사
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            Users existingUser = userRepo.findByEmail(request.getEmail());
            if (existingUser != null) {
                throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getSrc() != null) {
            user.setSrc(request.getSrc());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }

        user.setUpdated(LocalDateTime.now());
        Users savedUser = userRepo.save(user);
        return toProfileResponse(savedUser);
    }

    @Override
    public void changePassword(String userId, PasswordChangeRequest request) {
        Users user = userRepo.findByUserId(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getUserPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdated(LocalDateTime.now());
        userRepo.save(user);
    }

    @Override
    public void deleteUser(String userId, UserDeleteRequest request) {
        Users user = userRepo.findByUserId(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setEnabled(false);
        user.setUpdated(LocalDateTime.now());
        userRepo.save(user);
    }

    private UserProfileResponse toProfileResponse(Users user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .src(user.getSrc())
                .birthday(user.getBirthday())
                .roles(user.getRoles())
                .created(user.getCreated())
                .updated(user.getUpdated())
                .build();
    }
}
