package server.koraveler.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {
    private static final byte[] SECRET_KEY = "harieshariesharieshaireshariesha".getBytes();
    private static final long ACCESS_TOKEN_EXPIRY = 60 * 60 * 1000; // 1 minute (테스트용)
    private static final long REFRESH_TOKEN_EXPIRY = 30 * 24 * 60 * 60 * 1000; // 30 days
    private static final SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;

    // static 제거하고 인스턴스 메서드로 변경
    public String generateAccessToken(String username) {
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(algorithm, SECRET_KEY)
                .compact();
        return token;
    }

    public String generateRefreshToken(String username) {
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                .signWith(algorithm, SECRET_KEY)
                .compact();
        return token;
    }

    // ✅ 수정: ExpiredJwtException을 그대로 던지도록 변경
    public Claims verifyToken(String token) throws ExpiredJwtException, JwtException {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return claimsJws.getBody();
        } catch (ExpiredJwtException e) {
            // ExpiredJwtException을 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 다른 JWT 관련 예외를 JwtException으로 변환
            throw new JwtException("Invalid token", e);
        }
    }

    // ✅ 추가: 토큰에서 사용자명 추출 (예외 처리 포함)
    public String extractUsername(String token) throws ExpiredJwtException, JwtException {
        return verifyToken(token).getSubject();
    }

    // 토큰에서 만료 날짜를 가져오는 메서드
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 토큰에서 특정 클레임을 가져오는 메서드
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 토큰에서 모든 클레임을 가져오는 메서드
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰이 만료되었는지 확인하는 메서드
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // 토큰을 검증하는 메서드
    public Boolean validateToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ 추가: UserDetails와 함께 토큰 검증
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = verifyToken(token);
            String username = claims.getSubject();
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}