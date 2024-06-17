package server.koraveler.users.service;

import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.model.Users;

public interface UsersService {
    public UsersDTO createUser(Users user) throws Exception;
    public UsersDTO getUser(String email) throws Exception;
    public UsersDTO getUserFromAccessToken() throws Exception;
}
