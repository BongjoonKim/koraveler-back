package server.koraveler.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import server.koraveler.user.model.User;
import server.koraveler.user.service.UserService;

import java.net.http.HttpResponse;
import java.util.HashMap;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/account")
    public Object getUser(@RequestParam("userId") String userId) {
        return null;
    }

    @PostMapping("/account")
    public ResponseEntity<User> createUser(
            @RequestBody User user
    ) {
        return ResponseEntity.ok(
                userService.createUser(user)
        );
    }


}
