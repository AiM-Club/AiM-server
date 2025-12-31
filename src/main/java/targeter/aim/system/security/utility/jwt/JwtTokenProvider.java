package targeter.aim.system.security.utility.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import targeter.aim.system.configuration.security.JwtProperties;
import targeter.aim.system.security.model.AuthDetails;
import targeter.aim.system.security.model.JwtDto;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtTokenProvider {

    private final Key secret;
    private final JwtProperties jwtProperties;

    public JwtDto.TokenData createRefreshToken(AuthDetails authDetails, String refreshUuid) {
        Claims claims = Jwts.claims().setSubject(authDetails.getName());
        claims.put("tokenType", "REFRESH");
        claims.put("refreshUuid", refreshUuid);

        LocalDateTime expireAt = LocalDateTime.now().plusWeeks(jwtProperties.getRefreshTokenExpirationWeeks());
        String tokenString = buildTokenString(claims, expireAt);

        return JwtDto.TokenData.builder()
                .tokenString(tokenString)
                .expireAt(expireAt)
                .build();
    }

    public JwtDto.TokenData createAccessToken(AuthDetails authDetails, String refreshUuid) {
        Claims claims = Jwts.claims().setSubject(authDetails.getName());
        claims.put("tokenType", "ACCESS");
        claims.put("refreshUuid", refreshUuid);

        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(jwtProperties.getAccessTokenExpirationMinutes());
        String tokenString = buildTokenString(claims, expireAt);

        return JwtDto.TokenData.builder()
                .tokenString(tokenString)
                .expireAt(expireAt)
                .build();
    }

    public JwtDto.TokenPair createTokenPair(AuthDetails authDetails) {
        String refreshUuid = UUID.randomUUID().toString();

        JwtDto.TokenData refreshToken = createRefreshToken(authDetails, refreshUuid);
        JwtDto.TokenData accessToken = createAccessToken(authDetails, refreshUuid);

        return JwtDto.TokenPair.of(refreshToken, accessToken);
    }

    private String buildTokenString(Claims claims, LocalDateTime expireAt) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expireAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(secret, SignatureAlgorithm.HS256)
                .compact();
    }
}