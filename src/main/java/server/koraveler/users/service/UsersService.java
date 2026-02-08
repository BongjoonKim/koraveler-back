package server.koraveler.users.service;

import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.dto.request.PasswordChangeRequest;
import server.koraveler.users.dto.request.UserDeleteRequest;
import server.koraveler.users.dto.request.UserUpdateRequest;
import server.koraveler.users.dto.response.UserProfileResponse;
import server.koraveler.users.model.Users;

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
