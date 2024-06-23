package server.koraveler.users.service;

import server.koraveler.users.dto.TokenDTO;
import server.koraveler.users.dto.UsersDTO;

public interface LoginService {
    public UsersDTO login(UsersDTO usersDTO);

    public TokenDTO refreshToken(String refreshToken) throws Exception;

    // 로그인한 사용자 불러오기
    public UsersDTO loginUser() throws Exception;
}
