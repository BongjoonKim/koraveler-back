package server.koraveler.users.service;

import server.koraveler.users.dto.UsersDTO;

public interface LoginService {
    public UsersDTO login(UsersDTO usersDTO);
}
