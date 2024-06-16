package server.koraveler.users.service;

import server.koraveler.users.dto.TokenDTO;
import server.koraveler.users.dto.UsersDTO;

public interface LoginService {
    public UsersDTO login(UsersDTO usersDTO);

    public TokenDTO refreshToken(String refreshToken) {

    }
}
