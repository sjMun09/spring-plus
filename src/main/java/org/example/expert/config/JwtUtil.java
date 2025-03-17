package org.example.expert.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        try {
            String cutSecretKey = secretKey.replaceAll("\\s","");
            byte[] bytes = Base64.getDecoder().decode(cutSecretKey);
            this.key = Keys.hmacShaKeyFor(bytes);
            log.info("jwtUtil, jwt 키가 정상적으로 설정됨");
        } catch (IllegalArgumentException e) {
            log.error("jwt 시크릿 키 오류 : 올바른 base64 인지 확인", e);
            throw new ServerException("jwt 키 설정 중 오류 발생함: 확인해야함");
        }
    }

    public String createToken(Long userId, String email, String nickname, UserRole userRole) {
        Date date = new Date();

        return BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(String.valueOf(userId))
                        .claim("email", email)
                        .claim("nickname", nickname)
                        .claim("userRole", userRole.name())
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME))
                        .setIssuedAt(date) // 발급일
                        .signWith(key, signatureAlgorithm) // 암호화 알고리즘
                        .compact();
    }

    public String getNicknameFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("nickname", String.class);
    }

    public String substringToken(String tokenValue) {
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring(BEARER_PREFIX.length()).trim();
        }
        throw new ServerException("토큰이 올바르지 x");
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰: {}", e.getMessage());
            throw new ServerException("만료된 JWT 토큰입니다.");
        } catch (MalformedJwtException e) {
            log.error("잘못된 JWT 형식: {}", e.getMessage());
            throw new ServerException("잘못된 JWT 형식.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰: {}", e.getMessage());
            throw new ServerException("지원되지 않는 JWT 토큰.");
        } catch (SecurityException | SignatureException e) {
            log.error("JWT 서명 검증 실패: {}", e.getMessage());
            throw new ServerException("유효하지 않은 JWT 서명.");
        } catch (io.jsonwebtoken.io.DecodingException e) {
            log.error("JWT 디코딩 오류-Base64 URL 인코딩 문제: {}", e.getMessage());
            throw new ServerException("JWT 디코딩 오류: Base64 형식이 잘못되었습니다.");
        } catch (Exception e) {
            log.error("JWT 처리 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new ServerException("JWT 처리 중 알 수 없는 오류가 발생했습니다.");
        }
    }
}
