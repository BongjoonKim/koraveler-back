package server.nadeliv.users.service;

import server.nadeliv.error.CustomException;
import server.nadeliv.users.dto.TokenDTO;
import server.nadeliv.users.dto.UsersDTO;

public interface LoginService {
    public TokenDTO refreshToken(String refreshToken) throws CustomException, Exception;

    // 로그인한 사용자 불러오기
    public UsersDTO loginUser() throws Exception;

     void logout() throws Exception;

     // 사용자 회원가입
    UsersDTO createUser(UsersDTO usersDTO) throws Exception;
}
