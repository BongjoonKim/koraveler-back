package server.koraveler.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {
    private static final byte[] SECRET_KEY = "harieshariesharieshaireshariesha".getBytes();
    private static final long ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000; // 15분
    private static final long REFRESH_TOKEN_EXPIRY = 30L * 24 * 60 * 60 * 1000; // 30일 (L 추가 중요!)
    private static final SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;

    // 날짜 포맷터
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // 한국 시간대
    }

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY);

        // 디버깅 로그
        log.info("=== Generating Access Token ===");
        log.info("Username: {}", username);
        log.info("Current time: {} ({})", dateFormat.format(now), now.getTime());
        log.info("Expiry time: {} ({})", dateFormat.format(expiryDate), expiryDate.getTime());
        log.info("Token validity: {} ms ({} minutes)", ACCESS_TOKEN_EXPIRY, ACCESS_TOKEN_EXPIRY / 60000);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(algorithm, SECRET_KEY)
                .compact();

        log.info("Access Token generated successfully");
        return token;
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRY);

        // 디버깅 로그
        log.info("=== Generating Refresh Token ===");
        log.info("Username: {}", username);
        log.info("Current time: {} ({})", dateFormat.format(now), now.getTime());
        log.info("Expiry time: {} ({})", dateFormat.format(expiryDate), expiryDate.getTime());
        log.info("Token validity: {} ms ({} days)", REFRESH_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY / (24 * 60 * 60 * 1000));

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(algorithm, SECRET_KEY)
                .compact();

        log.info("Refresh Token generated successfully");
        return token;
    }

    public Claims verifyToken(String token) throws ExpiredJwtException, JwtException {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();

            // 토큰 정보 로깅
            log.debug("Token verified successfully");
            log.debug("Subject: {}", claims.getSubject());
            log.debug("Issued at: {}", dateFormat.format(claims.getIssuedAt()));
            log.debug("Expiry: {}", dateFormat.format(claims.getExpiration()));

            return claims;
        } catch (ExpiredJwtException e) {
            // 만료된 토큰 정보 로깅
            Claims expiredClaims = e.getClaims();
            Date now = new Date();
            Date expiry = expiredClaims.getExpiration();

            log.error("=== Token Expired ===");
            log.error("Current time: {} ({})", dateFormat.format(now), now.getTime());
            log.error("Token expiry: {} ({})", dateFormat.format(expiry), expiry.getTime());
            log.error("Time difference: {} ms ({} hours)",
                    now.getTime() - expiry.getTime(),
                    (now.getTime() - expiry.getTime()) / (1000 * 60 * 60));
            log.error("Token subject: {}", expiredClaims.getSubject());

            throw e;
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    // RefreshToken 전용 검증 메서드 (만료 시에도 일부 정보 반환)
    public Claims verifyRefreshToken(String token) {
        try {
            return verifyToken(token);
        } catch (ExpiredJwtException e) {
            Claims expiredClaims = e.getClaims();
            Date now = new Date();
            Date expiry = expiredClaims.getExpiration();
            long diffInDays = (now.getTime() - expiry.getTime()) / (1000 * 60 * 60 * 24);

            // RefreshToken이 만료된 지 7일 이내면 갱신 허용 (선택적)
            if (diffInDays <= 7) {
                log.warn("RefreshToken expired {} days ago but within grace period", diffInDays);
                return expiredClaims;
            } else {
                log.error("RefreshToken expired {} days ago, beyond grace period", diffInDays);
                throw new JwtException("RefreshToken expired beyond grace period");
            }
        }
    }

    public String extractUsername(String token) throws ExpiredJwtException, JwtException {
        return verifyToken(token).getSubject();
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 Claims 반환
            return e.getClaims();
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = verifyToken(token);
            String username = claims.getSubject();
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // 시스템 시간 확인용 메서드
    public void checkSystemTime() {
        Date now = new Date();
        log.info("=== System Time Check ===");
        log.info("Current system time: {}", dateFormat.format(now));
        log.info("Current timestamp: {}", now.getTime());
        log.info("Timezone: {}", TimeZone.getDefault().getID());
    }
}