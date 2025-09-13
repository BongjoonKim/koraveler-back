package server.koraveler.users.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import server.koraveler.users.service.CustomUserDetailsServiceImpl.CustomUserDetailService;
import server.koraveler.utils.JwtUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 인증이 필요 없는 경로들
        if (shouldSkipAuthentication(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        final String requestTokenHeader = request.getHeader("Authorization");

        // Bearer 토큰이 없는 경우
        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            log.warn("JWT Token does not begin with Bearer String");
            chain.doFilter(request, response);
            return;
        }

        String accessToken = requestTokenHeader.substring(7);

        try {
            // ✅ 인스턴스 메서드로 호출
            Claims claims = jwtTokenUtil.verifyToken(accessToken);
            String username = claims.getSubject();

            // SecurityContext에 인증 정보가 없는 경우에만 설정
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

                // 토큰 유효성 추가 검증
                if (jwtTokenUtil.validateToken(accessToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Authentication set for user: {}", username);
                }
            }

            // 인증 성공 시 다음 필터로 진행
            chain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.error("JWT Token has expired", e);
            sendUnauthorizedError(response, "Token expired");
            // ✅ 중요: return으로 필터 체인 중단
            return;

        } catch (JwtException e) {
            log.error("Invalid JWT Token", e);
            sendUnauthorizedError(response, "Invalid token");
            return;

        } catch (Exception e) {
            log.error("Unable to process JWT Token", e);
            sendUnauthorizedError(response, "Authentication failed");
            return;
        }
    }

    private boolean shouldSkipAuthentication(String path, String method) {
        return "OPTIONS".equalsIgnoreCase(method) ||
                path.contains("ps") ||
                path.equals("/") ||
                path.equals("/health") ||
                path.startsWith("/actuator") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/refresh");
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", message);
        errorDetails.put("status", 401);
        errorDetails.put("timestamp", System.currentTimeMillis());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), errorDetails);
    }
}