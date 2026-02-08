package server.koraveler.users.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.koraveler.users.dto.CustomUserDetails;
import server.koraveler.users.dto.request.PasswordChangeRequest;
import server.koraveler.users.dto.request.UserDeleteRequest;
import server.koraveler.users.dto.request.UserUpdateRequest;
import server.koraveler.users.dto.response.UserProfileResponse;
import server.koraveler.users.service.UsersService;

@RestController
@RequestMapping("/api/v1/user")
@Slf4j
public class UsersController {

    @Autowired
    private UsersService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserUpdateRequest request
    ) {
        UserProfileResponse profile = userService.updateUserProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PasswordChangeRequest request
    ) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserDeleteRequest request
    ) {
        userService.deleteUser(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}
