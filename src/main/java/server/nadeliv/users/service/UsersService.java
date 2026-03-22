package server.nadeliv.users.service;

import server.nadeliv.users.dto.UsersDTO;
import server.nadeliv.users.dto.request.PasswordChangeRequest;
import server.nadeliv.users.dto.request.UserDeleteRequest;
import server.nadeliv.users.dto.request.UserUpdateRequest;
import server.nadeliv.users.dto.response.UserProfileResponse;
import server.nadeliv.users.model.Users;

public interface UsersService {
    public UsersDTO createUser(Users user) throws Exception;
    public UsersDTO getUser(String email) throws Exception;
    public UsersDTO getUserFromAccessToken() throws Exception;
    public UsersDTO updateUser(Users user) throws Exception;

    UserProfileResponse getUserProfile(String userId);
    UserProfileResponse updateUserProfile(String userId, UserUpdateRequest request);
    void changePassword(String userId, PasswordChangeRequest request);
    void deleteUser(String userId, UserDeleteRequest request);
}
