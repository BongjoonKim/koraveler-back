package server.koraveler.users.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    @Value("${jwt.secretKey}")
    private String secretKey;


    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // 인증 성공 후 JWT 토큰 생성
        String token = tokenProvider.createToken(authResult);

        // 응답 헤더에 토큰 추가
        response.addHeader("Authorization", "Bearer " + token);

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authResult);

        // 다음 필터로 요청 전달
        chain.doFilter(request, response);
    }
}