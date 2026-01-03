package targeter.aim.system.security.utility.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

    public JwtDto.TokenData createRefreshToken(AuthDetails authDetails) {
        String refreshUuid = UUID.randomUUID().toString();
        return createRefreshToken(authDetails, refreshUuid);
    }

    private JwtDto.TokenData createRefreshToken(AuthDetails authDetails, String refreshUuid) {
        Claims claims = Jwts.claims().setSubject(authDetails.getName());
        claims.put("tokenType", "REFRESH");
        claims.put("refreshUuid", refreshUuid);

        LocalDateTime expireLocalDateTime =
                LocalDateTime.now().plusWeeks(jwtProperties.getRefreshTokenExpirationWeeks());

        String tokenString = buildTokenString(claims, expireLocalDateTime);

        return JwtDto.TokenData.builder()
                .tokenString(tokenString)
                .expireAt(expireLocalDateTime)
                .build();
    }

    public JwtDto.TokenData createAccessToken(AuthDetails authDetails, String refreshUuid) {
        Claims claims = Jwts.claims().setSubject(authDetails.getName());
        claims.put("tokenType", "ACCESS");
        claims.put("refreshUuid", refreshUuid);

        LocalDateTime expireLocalDateTime =
                LocalDateTime.now().plusMinutes(jwtProperties.getAccessTokenExpirationMinutes());

        String tokenString = buildTokenString(claims, expireLocalDateTime);

        return JwtDto.TokenData.builder()
                .tokenString(tokenString)
                .expireAt(expireLocalDateTime)
                .build();
    }

    public JwtDto.TokenPair createTokenPair(AuthDetails authDetails) {
        String refreshUuid = UUID.randomUUID().toString();

        JwtDto.TokenData refreshTokenData = createRefreshToken(authDetails, refreshUuid);
        JwtDto.TokenData accessTokenData = createAccessToken(authDetails, refreshUuid);

        return JwtDto.TokenPair.of(refreshTokenData, accessTokenData);
    }

    private String buildTokenString(Claims claims, LocalDateTime expireAt) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expireAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(secret)
                .compact();
    }
}