package server.koraveler.users.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import server.koraveler.config.AuthenticationRequest;
import server.koraveler.config.AuthenticationResponse;
import server.koraveler.users.dto.UsersDTO;
import server.koraveler.users.service.CustomUserDetailsServiceImpl.CustomUserDetailService;
import server.koraveler.users.service.LoginService;
import server.koraveler.utils.JwtUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
@Slf4j
public class LoginController{
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    @Autowired
    private LoginService loginService;

    @Autowired
    private PasswordEncoder passwordEncoder;

//    @PostMapping("")
//    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
//        try {
//            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
//                    authenticationRequest.getUsername(),
//                    authenticationRequest.getPassword()
//            );
//            System.out.println("authenticationToken = " + passwordEncoder.encode(authenticationRequest.getPassword()));
//            Authentication authentication = authenticationManager.authenticate(authenticationToken);
//            System.out.println("authentication = " + authentication);
////            SecurityContextHolder.getContext().setAuthentication(authentication);
//
////            final UserDetails userDetails = customUserDetailService
////                    .loadUserByUsername(authenticationRequest.getUsername());
////
////            final String token = jwtTokenUtil.generateToken(userDetails);
//
//            return ResponseEntity.ok(null);
//
//        } catch (BadCredentialsException e) {
//            throw new Exception("Incorrect username or password", e);
//        }
//    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            OAuth2AuthenticationToken auth2AuthenticationToken
    ) {
        // refresh token 검증

        // refresh token도 만료되었을 때, 재로그인 화면으로

        //
        String username = auth

        return ResponseEntity.ok(
                loginService.login(usersDTO)
        );
    }
}
