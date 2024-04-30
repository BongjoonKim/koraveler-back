package server.koraveler.users.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.koraveler.users.model.Users;
import server.koraveler.users.service.UsersService;

@RestController
@RequestMapping("/user")
@Slf4j
public class UsersController {
    @Autowired
    private UsersService userService;

    @GetMapping("")
    public Object getUser() {
        return null;
    }

    @GetMapping("/account")
    public Object getUser(@RequestParam("userId") String userId) {
        return null;
    }

    @PostMapping("/account")
    public ResponseEntity<Users> createUser(
//            @RequestBody Users user
    ) {
        try{
            return ResponseEntity.ok(
                    userService.createUser(null)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }


}
