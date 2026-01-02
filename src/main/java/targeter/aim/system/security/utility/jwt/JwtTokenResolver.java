package targeter.aim.system.security.utility.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import targeter.aim.system.security.exception.JwtInvalidTokenException;
import targeter.aim.system.security.exception.JwtParseException;
import targeter.aim.system.security.exception.JwtTokenExpiredException;
import targeter.aim.system.security.model.JwtDto;
import java.time.ZoneId;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtTokenResolver {
    private final Key secret;

    public Optional<String> parseTokenFromRequest(HttpServletRequest request) {
        Optional<String> bearerToken;
        String headerValue;
        try {
            bearerToken = Optional.ofNullable(request.getHeader("Authorization"));
        }catch (Exception ignored) {
            bearerToken = Optional.empty();
        }
        return bearerToken
                .filter(token -> token.startsWith("Bearer"))
                .map(token -> token.substring(7));
    }

    private Jws<Claims> parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            throw new JwtTokenExpiredException(e);
        } catch (SignatureException e) {
            throw new JwtInvalidTokenException(e);
        } catch (Exception e) {
            throw new JwtParseException(e);
        }
    }

    public JwtDto.ParsedTokenData resolveTokenFromString(String token) {
        var parsed = parseClaims(token);
        return JwtDto.ParsedTokenData.builder()
                .subject(parsed.getBody().getSubject())
                .expireAt(parsed.getBody().getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .refreshUuid(parsed.getBody().get("refreshUuid", String.class))
                .build();
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }
}
