package server.koraveler.users.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.*;
import server.koraveler.config.AuthenticationRequest;
import server.koraveler.config.AuthenticationResponse;
import server.koraveler.error.CustomException;
import server.koraveler.error.ErrorResponse;
import server.koraveler.users.dto.TokenDTO;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.service.CustomUserDetailsServiceImpl.CustomUserDetailService;
import server.koraveler.users.service.LoginService;
import server.koraveler.utils.JwtUtil;

import java.util.*;

@RestController
@RequestMapping("ps/login")
@RequiredArgsConstructor
@Slf4j
public class LoginController{
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginService loginService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody HashMap<String, String> data
            ) {
        try {
            return ResponseEntity.ok(loginService.refreshToken(data.get("refreshToken")));
        } catch (CustomException e) {
            return new ResponseEntity<>(new CustomException(e.getStatus(), e.getMsg()), e.getStatus());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getLoginUser(
    ) {
        try {
            return ResponseEntity.ok(loginService.loginUser());
        } catch (Exception e) {
            return new ResponseEntity<>("ssssss", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> getLogout() {
        try {
            return ResponseEntity.ok(loginService.logout());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}
