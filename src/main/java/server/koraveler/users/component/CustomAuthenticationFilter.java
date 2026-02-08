package server.koraveler.users.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import server.koraveler.users.dto.TokenDTO;
import server.koraveler.utils.JwtUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl("/ps/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        System.out.println("Attempting authentication for request: " + request.getRequestURI());

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        System.out.println("Username: " + username); // 디버깅용

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        // 인증 수행
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // ✅ 핵심 수정: authentication 객체를 저장 (authenticationToken이 아님!)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        System.out.println("Authentication successful for user: " + username);

        return authentication;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {
        System.out.println("Processing successful authentication");

        UserDetails userDetails = (UserDetails) authResult.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setGrantType(userDetails.getAuthorities());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        response.setHeader("accessToken", accessToken);
        response.setHeader("refreshToken", refreshToken);
        response.setHeader("authorities", userDetails.getAuthorities().toString());

        new ObjectMapper().writeValue(response.getOutputStream(), tokenDTO);

        System.out.println("Token generated successfully for user: " + userDetails.getUsername());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {
        System.err.println("Authentication failed: " + failed.getMessage());
        failed.printStackTrace(); // 디버깅용

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", HttpStatus.UNAUTHORIZED.value());
        body.put("error", failed.getMessage());
        body.put("message", "Invalid username or password");

        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}