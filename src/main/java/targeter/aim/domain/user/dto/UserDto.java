package targeter.aim.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDateTime;

public class UserDto {
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class UserResponse {
        private String email;
        private String nickname;
        private String socialLogin;
        private LocalDateTime createdAt;
        private LocalDateTime lastModifiedAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .socialLogin(user.getSocialLogin() == null ? null : user.getSocialLogin().name())
                    .createdAt(user.getCreatedAt())
                    .lastModifiedAt(user.getLastModifiedAt())
                    .build();
        }
    }

}
