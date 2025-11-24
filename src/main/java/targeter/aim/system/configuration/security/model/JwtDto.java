package targeter.aim.system.configuration.security.model;

import lombok.*;

import java.time.LocalDateTime;

public class JwtDto {
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class TokenData {
        private String tokenString;
        private LocalDateTime expireAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
        private LocalDateTime accessTokenExpireAt;
        private LocalDateTime refreshTokenExpireAt;
    }
}
