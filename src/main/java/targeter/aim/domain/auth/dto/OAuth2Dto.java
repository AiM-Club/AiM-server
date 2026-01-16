package targeter.aim.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.system.security.model.JwtDto;

public class OAuth2Dto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "구글 로그인 요청 DTO")
    public static class GoogleLoginRequest {
        @NotBlank(message = "인가 코드를 입력해주세요.")
        @Schema(description = "Google 인증 서버로부터 받은 인가 코드", example = "4/0AfJohX...")
        private String code;

        @NotBlank(message = "redirectUri를 입력해주세요.")
        @Schema(description = "구글 콘솔에 등록된 Redirect URI (검증용)", example = "http://localhost:8080/login/oauth2/code/google")
        private String redirectUri;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "카카오 로그인 요청 DTO")
    public static class KakaoLoginRequest {
        @NotBlank(message = "인가 코드를 입력해주세요.")
        @Schema(description = "카카오 인증 서버로부터 받은 인가 코드", example = "authorization_code")
        private String code;

        @NotBlank(message = "redirectUri를 입력해주세요.")
        @Schema(description = "카카오 developers에 등록된 Redirect URI (검증용)", example = "http://localhost:8080/oauth/kakao/callback")
        private String redirectUri;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "소셜 로그인 응답 DTO")
    public static class SocialSignInResponse {
        @Schema(description = "로그인한 사용자 정보", implementation = UserDto.UserResponse.class)
        private UserDto.UserResponse user;

        @Schema(description = "발급된 토큰 정보", implementation = JwtDto.TokenInfo.class)
        private JwtDto.TokenInfo token;

        @Schema(description = "신규 소셜유저 여부(추가 정보 입력 필요)", example = "true")
        private Boolean isNewUser;

        public static OAuth2Dto.SocialSignInResponse of(UserDto.UserResponse user, JwtDto.TokenInfo token, Boolean isNewUser) {
            return OAuth2Dto.SocialSignInResponse.builder()
                    .user(user)
                    .token(token)
                    .isNewUser(isNewUser)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Hidden
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;

        private String scope;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Hidden
    public static class KakaoUserResponse {
        private Long id;

        @JsonProperty("kakao_account")
        private OAuth2Dto.KakaoUserResponse.KakaoAccount kakaoAccount;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KakaoAccount {
            private String email;

            private OAuth2Dto.KakaoUserResponse.KakaoAccount.Profile profile;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Profile {
                private String nickname;

                @JsonProperty("profile_image_url")
                private String profileImageUrl;
            }
        }
    }
}
