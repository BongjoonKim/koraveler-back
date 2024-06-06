package server.koraveler.users.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.service.LoginService;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("")
    public ResponseEntity<UsersDTO> login(
            @RequestBody UsersDTO usersDTO) {
        UsersDTO usersDTO1 = loginService.login(usersDTO);
        return ResponseEntity.ok(
                loginService.login(usersDTO)
        );
    }
}
