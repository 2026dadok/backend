package dongsim.dadok.auth.domain.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenValidity) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public String createAccessToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .subject(userId)
                .claim(TYPE_CLAIM, ACCESS)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .subject(userId)
                .id(java.util.UUID.randomUUID().toString()) // 같은 초에 발급해도 토큰이 서로 다르게
                .claim(TYPE_CLAIM, REFRESH)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 토큰 종류 구분 (access 토큰만 API 인증에 쓰고, refresh 토큰은 재발급에만 쓴다)
    public static final String TYPE_CLAIM = "typ";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    public boolean isAccessToken(String token) {
        return ACCESS.equals(getType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH.equals(getType(token));
    }

    private String getType(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload().get(TYPE_CLAIM, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}