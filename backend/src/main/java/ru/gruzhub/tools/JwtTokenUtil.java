package ru.gruzhub.tools;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.gruzhub.tools.env.EnvVariables;
import ru.gruzhub.users.models.User;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {
    private static final long JWT_TOKEN_VALIDITY = 100L * 365 * 60 * 60 * 1000; // 100 years

    private final EnvVariables envVariables;

    public String generateToken(User user) {
        String jwtSecretKey = this.envVariables.JWT_SECRET_KEY;
        return Jwts.builder()
                   .claim("id", user.getId())
                   .claim("password_creation_time", user.getPasswordCreationTime())
                   .setIssuedAt(new Date())
                   .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                   .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes()), SignatureAlgorithm.HS256)
                   .compact();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(this.envVariables.JWT_SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    public Claims getClaimsFromToken(String token) {
        String jwtSecretKey = this.envVariables.JWT_SECRET_KEY;
        return Jwts.parserBuilder()
                   .setSigningKey(Keys.hmacShaKeyFor(jwtSecretKey.getBytes()))
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }
}
