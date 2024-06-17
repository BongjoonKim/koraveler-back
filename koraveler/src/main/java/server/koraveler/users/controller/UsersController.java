package server.koraveler.users.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.model.Users;
import server.koraveler.users.service.UsersService;

@RestController
@RequestMapping("/user")
@Slf4j
public class UsersController {
    @Autowired
    private UsersService userService;

    @GetMapping("")
    public ResponseEntity<UsersDTO> getUserFromAccessToken() {
        try {
            return ResponseEntity.ok(userService.getUserFromAccessToken());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }

    @GetMapping("/account")
    public ResponseEntity<UsersDTO> getUser(@RequestParam("email") String email) {
        try {
            UsersDTO usersDTO = userService.getUser(email);
            return ResponseEntity.ok(usersDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }

    @PostMapping("/account")
    public ResponseEntity<UsersDTO> createUser(
            @RequestBody Users user
    ) {
        try{
            return ResponseEntity.ok(
                    userService.createUser(user)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }
}
