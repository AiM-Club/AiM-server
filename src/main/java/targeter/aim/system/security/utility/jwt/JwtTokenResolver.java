package targeter.aim.system.security.utility.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import targeter.aim.system.security.exception.JwtInvalidTokenException;
import targeter.aim.system.security.exception.JwtParseException;
import targeter.aim.system.security.exception.JwtTokenExpiredException;
import targeter.aim.system.security.model.JwtDto;

import java.security.Key;
import java.time.ZoneId;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtTokenResolver {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Key secret;

    public Optional<String> parseTokenFromRequest(HttpServletRequest request) {
        String headerValue;
        try {
            headerValue = request.getHeader(AUTH_HEADER);
        } catch (Exception ignored) {
            return Optional.empty();
        }

        if (headerValue == null) return Optional.empty();

        // "Bearer " (공백 포함) 으로 시작하는지 체크
        if (!headerValue.startsWith(BEARER_PREFIX)) return Optional.empty();

        // prefix 제거 후 trim (혹시 모를 공백/개행 제거)
        String token = headerValue.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) return Optional.empty();

        return Optional.of(token);
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
