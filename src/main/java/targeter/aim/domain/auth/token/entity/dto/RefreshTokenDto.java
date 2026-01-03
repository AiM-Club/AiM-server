package targeter.aim.domain.auth.token.entity.dto;

import lombok.*;
import targeter.aim.domain.auth.token.entity.RefreshToken;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenDto {
    private String uuid;
    private String userKey;
    private LocalDateTime expiryDate;

    public static RefreshToken toEntity(
            String uuid,
            String userKey,
            LocalDateTime expiryDate
    ) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("RefreshToken uuid is null/blank. Check JwtTokenResolver claim mapping.");
        }

        return RefreshToken.builder()
                .uuid(uuid)
                .userKey(userKey)
                .expiryDate(expiryDate)
                .build();
    }
}